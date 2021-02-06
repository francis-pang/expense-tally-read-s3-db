package expense_tally.aws.csv_reader.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectId;
import expense_tally.aws.aurora.AuroraDatabaseConfiguration;
import expense_tally.aws.csv_reader.BankTransactionReader;
import expense_tally.aws.csv_reader.configuration.AppConfiguration;
import expense_tally.aws.csv_reader.configuration.ConfigurationParser;
import expense_tally.aws.database.SqlSessionFactory;
import expense_tally.aws.AppStartUpException;
import expense_tally.aws.log.ObjectToString;
import expense_tally.aws.s3.DatabaseS3EventAnalyzer;
import expense_tally.aws.s3.S3FileRetriever;
import expense_tally.csv.parser.CsvParser;
import expense_tally.expense_manager.persistence.ExpenseReadable;
import expense_tally.expense_manager.persistence.database.DatabaseEnvironmentId;
import expense_tally.expense_manager.persistence.database.ExpenseManagerTransactionDatabaseProxy;
import expense_tally.expense_manager.transformation.ExpenseTransactionTransformer;
import expense_tally.model.csv.AbstractCsvTransaction;
import expense_tally.model.persistence.transformation.ExpenseManagerTransaction;
import expense_tally.model.persistence.transformation.PaymentMethod;
import expense_tally.reconciliation.DiscrepantTransaction;
import expense_tally.reconciliation.ExpenseReconciler;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class App implements RequestHandler<S3Event, Void> {
  private static final Logger LOGGER = LogManager.getLogger(expense_tally.aws.em_change_processor.app.App.class);
  private static final int KNOWN_EXCEPTION_ERROR_CODE = 400;
  private static final int UNKNOWN_EXCEPTION_ERROR_CODE = 500;
  
  private BankTransactionReader bankTransactionReader;
  private AppConfiguration appConfiguration;

  public App() {
    try {
      init();
    } catch (AppStartUpException | IOException | SQLException exception) {
      LOGGER
          .atFatal()
          .withThrowable(exception)
          .log("Unable to initialise class.");
      System.exit(KNOWN_EXCEPTION_ERROR_CODE);
    } catch (Exception exception) {
      LOGGER
          .atFatal()
          .withThrowable(exception)
          .log("Unable to initialise class.");
      System.exit(UNKNOWN_EXCEPTION_ERROR_CODE);
    }
  }

  private void init() throws AppStartUpException, IOException, SQLException {
    LOGGER.atDebug().log("Initialising application");
    LOGGER.atDebug().log("Reading application configuration.");
    appConfiguration = ConfigurationParser.parseSystemEnvironmentVariableConfiguration();
    LOGGER.atDebug().log("Application configuration is loaded. appConfiguration:{}", appConfiguration);
    S3FileRetriever s3FileRetriever = assembleS3FileRetriever();
    ExpenseReadable expenseReadable = assembleExpenseReadable();
    File csvFile = retrieveCsvFile();
    bankTransactionReader = BankTransactionReader.create(s3FileRetriever, expenseReadable, csvFile);
  }

  @Override
  public Void handleRequest(S3Event s3Event, Context context) {
    try {
      bankTransactionReader.reconcile(s3Event);
    } catch (Exception exception) {
      LOGGER
          .atError()
          .withThrowable(exception)
          .log("Unable to handle s3 event. event:{}", ObjectToString.extractStringFromObject(s3Event));
    }
    LOGGER.atInfo().log("Processed this S3 event: {}", ObjectToString.extractStringFromObject(s3Event));
    return null;
  }

  private S3FileRetriever assembleS3FileRetriever() {
    AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();
    return S3FileRetriever.create(amazonS3);
  }

  private File retrieveCsvFile() {
    return appConfiguration.getCsvFile();
  }

  private ExpenseReadable assembleExpenseReadable() throws IOException, SQLException {
    AuroraDatabaseConfiguration auroraDatabaseConfiguration = retrieveAuroraDatabaseConfiguration();
    final String AURORA_DATABASE_URL = auroraDatabaseConfiguration.getDestinationDbHostUrl();
    final String EXPENSE_MANAGER_DATABASE_NAME = auroraDatabaseConfiguration.getDestinationDbName();
    final String AURORA_USERNAME = auroraDatabaseConfiguration.getDstntnDbUsername();
    final String AURORA_PASSWORD = auroraDatabaseConfiguration.getDstntnDbPassword();
    SqlSession sqlSession = SqlSessionFactory.constructSqlSession(DatabaseEnvironmentId.MYSQL, AURORA_DATABASE_URL,
        EXPENSE_MANAGER_DATABASE_NAME, AURORA_USERNAME, AURORA_PASSWORD);
    return new ExpenseManagerTransactionDatabaseProxy(sqlSession);
  }

  private AuroraDatabaseConfiguration retrieveAuroraDatabaseConfiguration() {
    return appConfiguration.getAuroraDatabaseConfiguration();
  }
}

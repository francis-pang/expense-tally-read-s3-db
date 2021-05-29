package expense_tally.aws.em_change_processor.configuration.configuration;

import expense_tally.aws.AppConfigEnum;
import expense_tally.aws.AppStartUpException;
import expense_tally.aws.aurora.AuroraConfigurationParser;
import expense_tally.aws.aurora.AuroraDatabaseConfiguration;

import java.util.Optional;

public class ConfigurationParser {

  /**
   * Make implicit constructor private as there is no need to initialise class
   */
  private ConfigurationParser() {
  }

  public static AppConfiguration parseSystemEnvironmentVariableConfiguration() throws AppStartUpException {
    AuroraDatabaseConfiguration auroraDatabaseConfiguration =
        AuroraConfigurationParser.parseSystemEnvironmentVariableConfigurations();
    AppConfiguration.Builder appConfigurationBuilder = new AppConfiguration.Builder(auroraDatabaseConfiguration);

    Optional<String> sourceDbFilePath = parseSingleConfiguration(AppConfigEnum.EXPENSE_MANAGER_FILE_PATH.key());
    if (sourceDbFilePath.isPresent()) {
      appConfigurationBuilder = appConfigurationBuilder.localDbFilePath(sourceDbFilePath.get());
    }
    Optional<String> sourceDbEnvId =
        parseSingleConfiguration(AppConfigEnum.EXPENSE_REPORT_ENVIRONMENTAL_ID.key());
    if (sourceDbEnvId.isPresent()) {
      appConfigurationBuilder = appConfigurationBuilder.sourceDbEnvId(sourceDbEnvId.get());
    }
    return appConfigurationBuilder.build();
  }

  private static Optional<String> parseSingleConfiguration(String keyName) {
    String nullableValue = System.getenv(keyName);
    return Optional.ofNullable(nullableValue);
  }
}
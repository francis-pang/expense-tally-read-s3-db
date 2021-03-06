package expense_tally.aws.csv_reader.configuration;

import expense_tally.aws.aurora.AuroraDatabaseConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.File;
import java.net.URI;
import java.util.Objects;
import java.util.StringJoiner;

/**
 *
 */
public class CsvReaderConfiguration {
  private static final String DEFAULT_CSV_FILE_PATH = URI.create("/tmp/transaction.csv").getPath();

  private final File csvFile;
  private final AuroraDatabaseConfiguration auroraDatabaseConfiguration;

  /**
   * Private constructor for static factory method design pattern
   * @param csvFile the csv file
   * @param auroraDatabaseConfiguration database configuration of the Aurora database
   */
  private CsvReaderConfiguration(File csvFile, AuroraDatabaseConfiguration auroraDatabaseConfiguration) {
    this.csvFile = Objects.requireNonNull(csvFile);
    this.auroraDatabaseConfiguration = Objects.requireNonNull(auroraDatabaseConfiguration);
  }

  public File getCsvFile() {
    return csvFile;
  }

  public AuroraDatabaseConfiguration getAuroraDatabaseConfiguration() {
    return auroraDatabaseConfiguration;
  }

  public static CsvReaderConfiguration create(AuroraDatabaseConfiguration auroraDatabaseConfiguration) {
    File file = new File(DEFAULT_CSV_FILE_PATH);
    return new CsvReaderConfiguration(file, auroraDatabaseConfiguration);
  }

  public static CsvReaderConfiguration create(String csvFilePath,
                                              AuroraDatabaseConfiguration auroraDatabaseConfiguration) {
    File file = new File(csvFilePath);
    return new CsvReaderConfiguration(file, auroraDatabaseConfiguration);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CsvReaderConfiguration that = (CsvReaderConfiguration) o;

    return new EqualsBuilder()
        .append(csvFile, that.csvFile)
        .append(auroraDatabaseConfiguration, that.auroraDatabaseConfiguration)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(csvFile)
        .append(auroraDatabaseConfiguration)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", CsvReaderConfiguration.class.getSimpleName() + "[", "]")
        .add("csvFile=" + csvFile)
        .add("auroraDatabaseConfiguration=" + auroraDatabaseConfiguration)
        .toString();
  }
}

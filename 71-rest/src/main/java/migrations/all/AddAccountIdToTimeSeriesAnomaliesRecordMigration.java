package migrations.all;

public class AddAccountIdToTimeSeriesAnomaliesRecordMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesAnomaliesRecords";
  }

  @Override
  protected String getFieldName() {
    return "accountId";
  }
}

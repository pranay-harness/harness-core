package migrations.all;

public class AddAccountIdToTimeSeriesCumulativeSums extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesCumulativeSums";
  }

  @Override
  protected String getFieldName() {
    return "accountId";
  }
}

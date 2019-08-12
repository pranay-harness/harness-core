package migrations.timescaledb;

public class AddRollbackToDeployment extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_rollback_to_deployment.sql";
  }
}

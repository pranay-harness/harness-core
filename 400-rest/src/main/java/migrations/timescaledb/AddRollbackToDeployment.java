package migrations.timescaledb;

public class AddRollbackToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_rollback_to_deployment.sql";
  }
}

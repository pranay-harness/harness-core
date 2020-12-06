package migrations.timescaledb;

public class AddInstancesDeployedToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_instances_deployed_to_deployment.sql";
  }
}

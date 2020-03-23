package migrations.timescaledb;

public class AddDeploymentTagsToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_deployment_tags_to_deployment.sql";
  }
}

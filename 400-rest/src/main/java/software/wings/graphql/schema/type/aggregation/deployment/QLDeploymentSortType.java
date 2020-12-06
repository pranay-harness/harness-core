package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;

public enum QLDeploymentSortType {
  Duration(DeploymentMetaDataFields.DURATION),
  Count(DeploymentMetaDataFields.COUNT),
  RollbackDuration(DeploymentMetaDataFields.ROLLBACK_DURATION);

  private DeploymentMetaDataFields deploymentMetadata;

  QLDeploymentSortType(DeploymentMetaDataFields deploymentMetadata) {
    this.deploymentMetadata = deploymentMetadata;
  }

  public DeploymentMetaDataFields getDeploymentMetadata() {
    return deploymentMetadata;
  }
}

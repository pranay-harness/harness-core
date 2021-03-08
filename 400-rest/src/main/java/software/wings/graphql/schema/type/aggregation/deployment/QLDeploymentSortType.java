package software.wings.graphql.schema.type.aggregation.deployment;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;

@TargetModule(Module._380_CG_GRAPHQL)
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

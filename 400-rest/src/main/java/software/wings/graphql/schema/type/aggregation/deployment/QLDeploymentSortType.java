package software.wings.graphql.schema.type.aggregation.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
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

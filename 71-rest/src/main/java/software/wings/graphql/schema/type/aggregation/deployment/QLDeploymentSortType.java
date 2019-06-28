package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;

public enum QLDeploymentSortType {
  Application(DeploymentMetaDataFields.APPID),
  Service(DeploymentMetaDataFields.SERVICEID),
  Environment(DeploymentMetaDataFields.ENVID),
  CloudProvider(DeploymentMetaDataFields.CLOUDPROVIDERID),
  Status(DeploymentMetaDataFields.STATUS),
  EndTime(DeploymentMetaDataFields.ENDTIME),
  StartTime(DeploymentMetaDataFields.STARTTIME),
  Duration(DeploymentMetaDataFields.DURATION),
  Triggered_By(DeploymentMetaDataFields.TRIGGERED_BY),
  Trigger(DeploymentMetaDataFields.TRIGGER_ID),
  Workflow(DeploymentMetaDataFields.WORKFLOWID),
  Pipeline(DeploymentMetaDataFields.PIPELINEID),
  Count(DeploymentMetaDataFields.COUNT);

  private DeploymentMetaDataFields deploymentMetadata;

  QLDeploymentSortType(DeploymentMetaDataFields deploymentMetadata) {
    this.deploymentMetadata = deploymentMetadata;
  }

  public DeploymentMetaDataFields getDeploymentMetadata() {
    return deploymentMetadata;
  }
}

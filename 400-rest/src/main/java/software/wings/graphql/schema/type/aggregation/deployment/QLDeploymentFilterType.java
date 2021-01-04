package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLDeploymentFilterType {
  Application(DeploymentMetaDataFields.APPID),
  Service(DeploymentMetaDataFields.SERVICEID),
  Environment(DeploymentMetaDataFields.ENVID),
  EnvironmentType(DeploymentMetaDataFields.ENVTYPE),
  CloudProvider(DeploymentMetaDataFields.CLOUDPROVIDERID),
  Status(DeploymentMetaDataFields.STATUS),
  EndTime(DeploymentMetaDataFields.ENDTIME),
  StartTime(DeploymentMetaDataFields.STARTTIME),
  Duration(DeploymentMetaDataFields.DURATION),
  RollbackDuration(DeploymentMetaDataFields.ROLLBACK_DURATION),
  TriggeredBy(DeploymentMetaDataFields.TRIGGERED_BY),
  Trigger(DeploymentMetaDataFields.TRIGGER_ID),
  Workflow(DeploymentMetaDataFields.WORKFLOWID),
  Pipeline(DeploymentMetaDataFields.PIPELINEID),
  Tag(null),
  Tags(DeploymentMetaDataFields.TAGS);

  private QLDataType dataType;
  private DeploymentMetaDataFields metaDataFields;
  QLDeploymentFilterType() {}

  QLDeploymentFilterType(DeploymentMetaDataFields metaDataFields) {
    this.metaDataFields = metaDataFields;
  }

  public DeploymentMetaDataFields getMetaDataFields() {
    return metaDataFields;
  }
}

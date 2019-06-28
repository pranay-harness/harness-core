package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLDeploymentFilterType {
  Application(QLDataType.STRING, DeploymentMetaDataFields.APPID),
  Service(QLDataType.STRING, DeploymentMetaDataFields.SERVICEID),
  Environment(QLDataType.STRING, DeploymentMetaDataFields.ENVID),
  CloudProvider(QLDataType.STRING, DeploymentMetaDataFields.CLOUDPROVIDERID),
  Status(QLDataType.STRING, DeploymentMetaDataFields.STATUS),
  EndTime(QLDataType.NUMBER, DeploymentMetaDataFields.ENDTIME),
  StartTime(QLDataType.NUMBER, DeploymentMetaDataFields.STARTTIME),
  Duration(QLDataType.NUMBER, DeploymentMetaDataFields.DURATION),
  Triggered_By(QLDataType.STRING, DeploymentMetaDataFields.TRIGGERED_BY),
  Trigger(QLDataType.STRING, DeploymentMetaDataFields.TRIGGER_ID),
  Workflow(QLDataType.STRING, DeploymentMetaDataFields.WORKFLOWID),
  Pipeline(QLDataType.STRING, DeploymentMetaDataFields.PIPELINEID);

  private QLDataType dataType;
  private DeploymentMetaDataFields metaDataFields;
  QLDeploymentFilterType() {}

  QLDeploymentFilterType(QLDataType dataType, DeploymentMetaDataFields metaDataFields) {
    this.dataType = dataType;
    this.metaDataFields = metaDataFields;
  }

  public QLDataType getDataType() {
    return dataType;
  }

  public DeploymentMetaDataFields getMetaDataFields() {
    return metaDataFields;
  }
}

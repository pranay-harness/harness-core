package software.wings.graphql.datafetcher.cloudefficiencyevents;

import software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields;

public enum QLEventsDataFilterType {
  Application(CEEventsMetaDataFields.APPID),
  StartTime(CEEventsMetaDataFields.STARTTIME),
  EndTime(CEEventsMetaDataFields.STARTTIME),
  Service(CEEventsMetaDataFields.SERVICEID),
  Environment(CEEventsMetaDataFields.ENVID),
  Cluster(CEEventsMetaDataFields.CLUSTERID),
  CloudServiceName(CEEventsMetaDataFields.CLOUDSERVICENAME),
  TaskId(CEEventsMetaDataFields.TASKID),
  WorkloadName(CEEventsMetaDataFields.WORKLOADNAME),
  Namespace(CEEventsMetaDataFields.NAMESPACE);

  private CEEventsMetaDataFields metaDataFields;
  QLEventsDataFilterType() {}

  QLEventsDataFilterType(CEEventsMetaDataFields metaDataFields) {
    this.metaDataFields = metaDataFields;
  }

  public CEEventsMetaDataFields getMetaDataFields() {
    return metaDataFields;
  }
}

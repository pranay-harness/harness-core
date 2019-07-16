package software.wings.graphql.datafetcher.execution;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLExecutionFilterType {
  Application(QLDataType.STRING),
  Service(QLDataType.STRING),
  Environment(QLDataType.STRING),
  CloudProvider(QLDataType.STRING),
  Status(QLDataType.STRING),
  EndTime(QLDataType.TIME),
  StartTime(QLDataType.TIME),
  CreatedAt(QLDataType.TIME),
  Duration(QLDataType.NUMBER),
  TriggeredBy(QLDataType.STRING),
  Trigger(QLDataType.STRING),
  Workflow(QLDataType.STRING),
  Pipeline(QLDataType.STRING);

  QLDataType dataType;

  QLExecutionFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}

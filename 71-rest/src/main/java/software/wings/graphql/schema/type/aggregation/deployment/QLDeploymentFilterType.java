package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;

public enum QLDeploymentFilterType {
  Application(QLDataType.STRING, QLFilterKind.SIMPLE),
  Service(QLDataType.STRING, QLFilterKind.ARRAY),
  Environment(QLDataType.STRING, QLFilterKind.ARRAY),
  CloudProvider(QLDataType.STRING, QLFilterKind.ARRAY),
  Status(QLDataType.STRING, QLFilterKind.SIMPLE),
  EndTime(QLDataType.NUMBER, QLFilterKind.TIME),
  StartTime(QLDataType.NUMBER, QLFilterKind.TIME),
  Duration(QLDataType.NUMBER, QLFilterKind.SIMPLE),
  Triggered_By(QLDataType.STRING, QLFilterKind.SIMPLE),
  Trigger(QLDataType.STRING, QLFilterKind.SIMPLE),
  Workflow(QLDataType.STRING, QLFilterKind.ARRAY),
  Pipeline(QLDataType.STRING, QLFilterKind.SIMPLE);

  private QLDataType dataType;
  private QLFilterKind filterKind;
  QLDeploymentFilterType() {}

  QLDeploymentFilterType(QLDataType dataType, QLFilterKind filterKind) {
    this.dataType = dataType;
    this.filterKind = filterKind;
  }

  public QLDataType getDataType() {
    return dataType;
  }

  public QLFilterKind getFilterKind() {
    return filterKind;
  }
}

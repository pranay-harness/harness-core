package software.wings.graphql.schema.type.aggregation.trigger;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLTriggerFilterType {
  Trigger(QLDataType.STRING),
  Application(QLDataType.STRING);

  private QLDataType dataType;

  QLTriggerFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}

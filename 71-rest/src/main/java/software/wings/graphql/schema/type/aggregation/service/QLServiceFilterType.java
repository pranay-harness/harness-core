package software.wings.graphql.schema.type.aggregation.service;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLServiceFilterType {
  Application(QLDataType.STRING),
  Service(QLDataType.STRING);

  private QLDataType dataType;

  QLServiceFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}

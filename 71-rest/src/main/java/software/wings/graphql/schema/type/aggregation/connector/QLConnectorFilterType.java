package software.wings.graphql.schema.type.aggregation.connector;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLConnectorFilterType {
  Connector(QLDataType.STRING),
  Type(QLDataType.STRING),
  CreatedAt(QLDataType.NUMBER);

  private QLDataType dataType;

  QLConnectorFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}

package software.wings.graphql.schema.type.aggregation.environment;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLEnvironmentFilterType {
  Environment(QLDataType.STRING),
  Application(QLDataType.STRING),
  EnvironmentType(QLDataType.STRING);

  private QLDataType dataType;

  QLEnvironmentFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}

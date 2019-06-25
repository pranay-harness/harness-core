package software.wings.graphql.schema.type.aggregation.cloudprovider;

import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLCloudProviderFilterType {
  CloudProvider(QLDataType.STRING),
  Type(QLDataType.STRING),
  CreatedAt(QLDataType.NUMBER);

  private QLDataType dataType;

  QLCloudProviderFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  public QLDataType getDataType() {
    return dataType;
  }
}

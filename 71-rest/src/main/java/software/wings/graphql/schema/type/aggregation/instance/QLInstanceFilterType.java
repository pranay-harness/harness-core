package software.wings.graphql.schema.type.aggregation.instance;

import software.wings.graphql.schema.type.aggregation.QLDataType;

/**
 * @author rktummala
 */
public enum QLInstanceFilterType {
  CreatedAt(QLDataType.NUMBER),
  Application(QLDataType.STRING),
  Service(QLDataType.STRING),
  Environment(QLDataType.STRING),
  CloudProvider(QLDataType.STRING),
  InstanceType(QLDataType.STRING);

  private QLDataType dataType;

  QLInstanceFilterType(QLDataType dataType) {
    this.dataType = dataType;
  }

  QLInstanceFilterType() {}

  public QLDataType getDataType() {
    return dataType;
  }
}

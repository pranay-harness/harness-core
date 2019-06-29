package software.wings.graphql.schema.type.aggregation.instance;

import software.wings.graphql.schema.type.aggregation.QLDataType;

/**
 * @author rktummala
 */
public enum QLInstanceFilterType {
  CreatedAt(QLDataType.NUMBER, "createdAt", "REPORTEDAT"),
  Application(QLDataType.STRING, "appId", "APPID"),
  Service(QLDataType.STRING, "serviceId", "SERVICEID"),
  Environment(QLDataType.STRING, "envId", "ENVID"),
  CloudProvider(QLDataType.STRING, "computeProviderId", "COMPUTEPROVIDERID"),
  InstanceType(QLDataType.STRING, "instanceType", "INSTANCETYPE");

  private QLDataType dataType;
  private String mongoDbFieldName;
  private String sqlDbFieldName;

  QLInstanceFilterType(QLDataType dataType, String mongoDbFieldName, String sqlDbFieldName) {
    this.dataType = dataType;
    this.mongoDbFieldName = mongoDbFieldName;
    this.sqlDbFieldName = sqlDbFieldName;
  }

  public QLDataType getDataType() {
    return dataType;
  }

  public String getMongoDbFieldName() {
    return mongoDbFieldName;
  }

  public String getSqlDbFieldName() {
    return sqlDbFieldName;
  }
}

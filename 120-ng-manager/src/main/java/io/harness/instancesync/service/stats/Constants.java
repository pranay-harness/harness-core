package io.harness.instancesync.service.stats;

public enum Constants {
  ACCOUNT_ID("ACCOUNTID"),
  SERVICE_ID("SERVICEID"),
  ENV_ID("ENVID"),
  CLOUDPROVIDER_ID("CLOUDPROVIDERID"),
  INFRAMAPPING_ID("INFRAMAPPINGID"),
  INSTANCE_TYPE("INSTANCETYPE"),
  ARTIFACT_ID("ARTIFACTID"),
  INSTANCECOUNT("INSTANCECOUNT"),
  ;

  private String key;

  Constants(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}

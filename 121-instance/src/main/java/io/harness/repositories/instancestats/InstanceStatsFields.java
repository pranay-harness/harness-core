package io.harness.repositories.instancestats;

public enum InstanceStatsFields {
  ACCOUNTID("ACCOUNTID"),
  ENVID("ENVID"),
  SERVICEID("SERVICEID"),
  REPORTEDAT("REPORTEDAT"),
  ;

  private String fieldName;

  InstanceStatsFields(String fieldName) {
    this.fieldName = fieldName;
  }

  public String fieldName() {
    return this.fieldName;
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

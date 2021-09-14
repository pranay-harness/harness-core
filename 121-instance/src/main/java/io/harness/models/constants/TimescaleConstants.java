/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.models.constants;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public enum TimescaleConstants {
  ACCOUNT_ID("ACCOUNTID"),
  ORG_ID("ORGID"),
  PROJECT_ID("PROJECTID"),
  SERVICE_ID("SERVICEID"),
  ENV_ID("ENVID"),
  CLOUDPROVIDER_ID("CLOUDPROVIDERID"),
  INFRAMAPPING_ID("INFRAMAPPINGID"),
  INSTANCE_TYPE("INSTANCETYPE"),
  ARTIFACT_ID("ARTIFACTID"),
  INSTANCECOUNT("INSTANCECOUNT"),
  SANITYSTATUS("SANITYSTATUS"),
  REPORTEDAT("REPORTEDAT");

  private String key;

  TimescaleConstants(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}

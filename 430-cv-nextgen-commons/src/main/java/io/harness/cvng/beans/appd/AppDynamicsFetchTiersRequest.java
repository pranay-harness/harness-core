/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.appd;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.delegate.beans.cvng.appd.AppDynamicsUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@JsonTypeName("APPDYNAMICS_FETCH_TIERS")
@Data
@SuperBuilder
@OwnedBy(CV)
public class AppDynamicsFetchTiersRequest extends AppDynamicsDataCollectionRequest {
  public static final String DSL = AppDynamicsDataCollectionRequest.readDSL(
      "appd-fetch-tiers.datacollection", AppDynamicsDataCollectionRequest.class);
  public AppDynamicsFetchTiersRequest() {
    setType(DataCollectionRequestType.APPDYNAMICS_FETCH_TIERS);
  }

  private String appName;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> envVariables = AppDynamicsUtils.getCommonEnvVariables(getConnectorConfigDTO());
    envVariables.put("appName", appName);
    return envVariables;
  }
}

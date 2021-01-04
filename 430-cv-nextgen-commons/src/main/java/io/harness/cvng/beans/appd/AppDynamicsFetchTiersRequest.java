package io.harness.cvng.beans.appd;

import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@JsonTypeName("APPDYNAMICS_FETCH_TIERS")
@Data
@SuperBuilder
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
    Map<String, Object> envVariables = new HashMap<>();
    envVariables.put("appName", appName);
    return envVariables;
  }
}

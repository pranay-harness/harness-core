package io.harness.cvng.beans.appd;

import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;

@JsonTypeName("APPDYNAMICS_FETCH_APPS")
@SuperBuilder
public class AppDynamicsFetchAppRequest extends AppDynamicsDataCollectionRequest {
  public static final String DSL = AppDynamicsDataCollectionRequest.readDSL(
      "appd-fetch-apps.datacollection", AppDynamicsDataCollectionRequest.class);
  public AppDynamicsFetchAppRequest() {
    setType(DataCollectionRequestType.APPDYNAMICS_FETCH_APPS);
  }
  @Override
  public String getDSL() {
    return DSL;
  }
}

package io.harness.cvng.beans.appd;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequestType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;

@JsonTypeName("APPDYNAMICS_FETCH_APPS")
@SuperBuilder
@OwnedBy(CV)
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

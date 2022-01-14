package io.harness.cvng.beans.datadog;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("DATADOG_DASHBOARD_DETAILS")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
@FieldNameConstants(innerTypeName = "DatadogDashboardDetailsRequestKeys")
public class DatadogDashboardDetailsRequest extends DatadogRequest {
  private static final String DSL =
      DataCollectionRequest.readDSL("datadog-dashboard-details.datacollection", DatadogDashboardDetailsRequest.class);
  private String dashboardId;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> commonEnvVariables = super.fetchDslEnvVariables();
    commonEnvVariables.put(DatadogDashboardDetailsRequestKeys.dashboardId, dashboardId);
    return commonEnvVariables;
  }
}

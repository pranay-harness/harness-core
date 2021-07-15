package io.harness.cvng.beans.stackdriver;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.cvng.utils.StackdriverUtils.Scope.METRIC_SCOPE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.utils.StackdriverUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("STACKDRIVER_DASHBOARD_GET")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class StackdriverDashboardDetailsRequest extends StackdriverRequest {
  public static final String DSL = StackdriverDashboardRequest.readDSL(
      "stackdriver-dashboards-details.datacollection", StackdriverDashboardRequest.class);

  private String path;

  @Override
  public String getBaseUrl() {
    return "https://monitoring.googleapis.com/v1/";
  }

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> envVariables = StackdriverUtils.getCommonEnvVariables(getConnectorConfigDTO(), METRIC_SCOPE);
    envVariables.put("path", path);
    return envVariables;
  }
}

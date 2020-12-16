package io.harness.cvng.beans.stackdriver;

import io.harness.cvng.utils.StackdriverUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("STACKDRIVER_DASHBOARD_GET")
@SuperBuilder
@NoArgsConstructor
@Data
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
    Map<String, Object> envVariables = new HashMap<>();
    StackdriverCredential credential = StackdriverCredential.fromGcpConnector(getConnectorConfigDTO());
    String jwtToken = StackdriverUtils.getJwtToken(credential);
    envVariables.put("jwtToken", jwtToken);
    envVariables.put("project", credential.getProjectId());
    envVariables.put("path", path);
    return envVariables;
  }
}

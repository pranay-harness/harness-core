package io.harness.cvng.beans.stackdriver;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.utils.StackdriverUtils;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public abstract class StackdriverRequest extends DataCollectionRequest<GcpConnectorDTO> {
  @Override
  public String getBaseUrl() {
    return "https://monitoring.googleapis.com/v1/projects/";
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    StackdriverCredential credential = StackdriverCredential.fromGcpConnector(getConnectorConfigDTO());
    return getCommonEnvVariables(credential);
  }

  protected Map<String, Object> getCommonEnvVariables(StackdriverCredential credential) {
    Map<String, Object> envVariables = new HashMap<>();
    String jwtToken = StackdriverUtils.getJwtToken(credential);
    envVariables.put("jwtToken", jwtToken);
    envVariables.put("project", credential.getProjectId());
    return envVariables;
  }
}
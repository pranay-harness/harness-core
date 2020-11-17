package io.harness.cvng.beans.splunk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public abstract class SplunkDataCollectionRequest extends DataCollectionRequest<SplunkConnectorDTO> {
  @Override
  public Map<String, String> collectionHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", SplunkUtils.getAuthorizationHeader(getConnectorConfigDTO()));
    return headers;
  }
  @Override
  public String getBaseUrl() {
    return getConnectorConfigDTO().getSplunkUrl();
  }
}

package io.harness.cvng.beans.splunk;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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

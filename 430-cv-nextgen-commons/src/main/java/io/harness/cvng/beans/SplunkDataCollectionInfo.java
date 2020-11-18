package io.harness.cvng.beans;

import io.harness.cvng.beans.splunk.SplunkUtils;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SplunkDataCollectionInfo extends LogDataCollectionInfo<SplunkConnectorDTO> {
  private String query;
  private String serviceInstanceIdentifier;
  @Override
  public Map<String, Object> getDslEnvVariables() {
    Map<String, Object> map = new HashMap<>();
    map.put("query", query);
    map.put("hostCollectionQuery", serviceInstanceIdentifier + "=*|stats count by " + serviceInstanceIdentifier);
    map.put("serviceInstanceIdentifier", "$." + serviceInstanceIdentifier);
    // TODO: setting max to 10000 now. We need to find a generic way to throw exception
    // in case of too many logs.
    map.put("maxCount", 10000);
    return map;
  }

  @Override
  public String getBaseUrl(SplunkConnectorDTO splunkConnectorDTO) {
    return splunkConnectorDTO.getSplunkUrl();
  }

  @Override
  public Map<String, String> collectionHeaders(SplunkConnectorDTO splunkConnectorDTO) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", SplunkUtils.getAuthorizationHeader(splunkConnectorDTO));
    return headers;
  }

  @Override
  public Map<String, String> collectionParams(SplunkConnectorDTO splunkConnectorDTO) {
    return Collections.emptyMap();
  }
}

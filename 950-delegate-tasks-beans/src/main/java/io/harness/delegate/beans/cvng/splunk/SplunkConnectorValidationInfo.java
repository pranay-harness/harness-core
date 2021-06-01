package io.harness.delegate.beans.cvng.splunk;

import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.cvng.ConnectorValidationInfo;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SplunkConnectorValidationInfo extends ConnectorValidationInfo<SplunkConnectorDTO> {
  private static final String DSL = readDSL("splunk-validation.datacollection", SplunkConnectorValidationInfo.class);
  @Override
  public String getConnectionValidationDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return connectorConfigDTO.getSplunkUrl();
  }

  @Override
  public Map<String, String> collectionHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", SplunkUtils.getAuthorizationHeader(connectorConfigDTO));
    return headers;
  }
}

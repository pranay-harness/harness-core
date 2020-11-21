package io.harness.cvng.beans.appd;

import io.harness.cvng.beans.ConnectorValidationInfo;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AppDynamicsConnectorValidationInfo extends ConnectorValidationInfo<AppDynamicsConnectorDTO> {
  private static final String DSL =
      readDSL("appdynamics-validation.datacollection", AppDynamicsConnectorValidationInfo.class);
  @Override
  public String getConnectionValidationDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return connectorConfigDTO.getControllerUrl();
  }

  @Override
  public Map<String, String> collectionHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", AppDynamicsUtils.getAuthorizationHeader(connectorConfigDTO));
    return headers;
  }
}

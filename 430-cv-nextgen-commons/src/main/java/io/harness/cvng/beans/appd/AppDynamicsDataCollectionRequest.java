package io.harness.cvng.beans.appd;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public abstract class AppDynamicsDataCollectionRequest extends DataCollectionRequest<AppDynamicsConnectorDTO> {
  @Override
  public Map<String, String> collectionHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", AppDynamicsUtils.getAuthorizationHeader(getConnectorConfigDTO()));
    return headers;
  }
  @Override
  public String getBaseUrl() {
    return getConnectorConfigDTO().getControllerUrl();
  }
}

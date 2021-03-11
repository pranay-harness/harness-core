package io.harness.cvng.beans.newrelic;

import io.harness.cvng.beans.ConnectorValidationInfo;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NewRelicConnectorValidationInfo extends ConnectorValidationInfo<NewRelicConnectorDTO> {
  private static final String BASE_URL = "v1/accounts/";
  private static final String DSL =
      readDSL("newrelic-validation.datacollection", NewRelicConnectorValidationInfo.class);
  @Override
  public String getConnectionValidationDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return getConnectorConfigDTO().getUrl() + BASE_URL + getConnectorConfigDTO().getNewRelicAccountId();
  }

  @Override
  public Map<String, String> collectionHeaders() {
    String apiKey = new String(getConnectorConfigDTO().getApiKeyRef().getDecryptedValue());
    Map<String, String> headers = new HashMap<>();
    headers.put("X-Query-Key", apiKey);
    headers.put("Accept", "application/json");
    return headers;
  }
}
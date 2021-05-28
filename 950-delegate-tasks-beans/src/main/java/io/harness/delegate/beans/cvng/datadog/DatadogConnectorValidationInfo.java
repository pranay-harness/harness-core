package io.harness.delegate.beans.cvng.datadog;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.delegate.beans.cvng.ConnectorValidationInfo;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DatadogConnectorValidationInfo extends ConnectorValidationInfo<DatadogConnectorDTO> {
  private static final String DSL = readDSL("datadog-validation.datacollection", DatadogConnectorValidationInfo.class);

  @Override
  public String getConnectionValidationDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return getMetricURLWithQueryParam(getConnectorConfigDTO().getUrl());
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return DatadogUtils.collectionHeaders(getConnectorConfigDTO());
  }

  private static String getMetricURLWithQueryParam(String url) {
    url += "metrics?from=" + Instant.now().getEpochSecond();
    return url;
  }
}

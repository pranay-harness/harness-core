package io.harness.cvng.beans.prometheus;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;

import java.util.Collections;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder
public abstract class PrometheusRequest extends DataCollectionRequest<PrometheusConnectorDTO> {
  @Override
  public String getBaseUrl() {
    return getConnectorConfigDTO().getUrl();
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return Collections.emptyMap();
  }
}

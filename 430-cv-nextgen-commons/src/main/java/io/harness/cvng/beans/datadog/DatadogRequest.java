package io.harness.cvng.beans.datadog;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.delegate.beans.cvng.datadog.DatadogUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@OwnedBy(CV)
public abstract class DatadogRequest extends DataCollectionRequest<DatadogConnectorDTO> {
  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    return new HashMap<>();
  }

  @Override
  public String getBaseUrl() {
    return getConnectorConfigDTO().getUrl();
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return DatadogUtils.collectionHeaders(getConnectorConfigDTO());
  }
}

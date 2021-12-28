package io.harness.cvng.beans.dynatrace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.delegate.beans.cvng.datadog.DatadogUtils;
import io.harness.delegate.beans.cvng.dynatrace.DynatraceUtils;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CV;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@OwnedBy(CV)
public abstract class DynatraceRequest extends DataCollectionRequest<DynatraceConnectorDTO> {
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
    return DynatraceUtils.collectionHeaders(getConnectorConfigDTO());
  }
}

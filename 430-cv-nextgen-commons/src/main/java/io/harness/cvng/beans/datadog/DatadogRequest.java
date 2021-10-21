package io.harness.cvng.beans.datadog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.delegate.beans.cvng.datadog.DatadogUtils;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CV;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@OwnedBy(CV)
public abstract class DatadogRequest extends DataCollectionRequest<DatadogConnectorDTO> {

    @Override
    public Map<String, Object> fetchDslEnvVariables() {
        return DatadogUtils.getCommonEnvVariables(getConnectorConfigDTO());
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

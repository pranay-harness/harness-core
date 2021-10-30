package io.harness.cvng.beans;

import io.harness.cvng.beans.datadog.DatadogLogDefinition;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.delegate.beans.cvng.datadog.DatadogUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.Map;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
public class DatadogLogDataCollectionInfo extends LogDataCollectionInfo<DatadogConnectorDTO> {
  DatadogLogDefinition logDefinition;

  @Override
  public Map<String, Object> getDslEnvVariables(DatadogConnectorDTO connectorConfigDTO) {
    Map<String, Object> dslEnvVariables = DatadogUtils.getCommonEnvVariables(connectorConfigDTO);
    dslEnvVariables.put("query", logDefinition.getQuery());
    // TODO slobodanpavic - map to string
    dslEnvVariables.put("indexes", logDefinition.getIndexes());
    dslEnvVariables.put("limit", 1000);

    return dslEnvVariables;
  }

  @Override
  public String getBaseUrl(DatadogConnectorDTO connectorConfigDTO) {
    return connectorConfigDTO.getUrl();
  }

  @Override
  public Map<String, String> collectionHeaders(DatadogConnectorDTO connectorConfigDTO) {
    return DatadogUtils.collectionHeaders(connectorConfigDTO);
  }

  @Override
  public Map<String, String> collectionParams(DatadogConnectorDTO connectorConfigDTO) {
   return Collections.emptyMap();
  }
}

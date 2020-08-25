package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cvng.models.VerificationType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.Data;

import java.util.Map;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DataCollectionInfo<T extends ConnectorConfigDTO> {
  private String dataCollectionDsl;
  private boolean collectHostData;
  public abstract VerificationType getVerificationType();
  public abstract Map<String, Object> getDslEnvVariables();
  public abstract String getBaseUrl(T connectorConfigDTO);
  public abstract Map<String, String> collectionHeaders(T connectorConfigDTO);
  public abstract Map<String, String> collectionParams(T connectorConfigDTO);
}

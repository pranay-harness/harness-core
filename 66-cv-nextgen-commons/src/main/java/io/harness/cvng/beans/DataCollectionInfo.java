package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cvng.models.VerificationType;
import lombok.Data;

import java.util.Map;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DataCollectionInfo {
  private String dataCollectionDsl;
  public abstract VerificationType getVerificationType();
  public abstract Map<String, Object> getDslEnvVariables();
}

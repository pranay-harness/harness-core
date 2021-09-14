/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DataCollectionInfo<T extends ConnectorConfigDTO> {
  private String dataCollectionDsl;
  private boolean collectHostData;
  public abstract VerificationType getVerificationType();
  public abstract Map<String, Object> getDslEnvVariables(T connectorConfigDTO);
  public abstract String getBaseUrl(T connectorConfigDTO);
  public abstract Map<String, String> collectionHeaders(T connectorConfigDTO);
  public abstract Map<String, String> collectionParams(T connectorConfigDTO);
}

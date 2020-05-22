package io.harness.cvng.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class DataCollectionInfo {
  public abstract String getType();
  public abstract Connector getConnector();
}

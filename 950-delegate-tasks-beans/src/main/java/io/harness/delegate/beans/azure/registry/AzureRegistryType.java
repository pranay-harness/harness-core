package io.harness.delegate.beans.azure.registry;

import io.harness.azure.model.AzureConstants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AzureRegistryType {
  @JsonProperty(AzureConstants.ACR) ACR("ACR"),
  @JsonProperty(AzureConstants.DOCKER_HUB_PUBLIC) DOCKER_HUB_PUBLIC("Docker Hub Public"),
  @JsonProperty(AzureConstants.DOCKER_HUB_PRIVATE) DOCKER_HUB_PRIVATE("Docker Hub Private"),
  @JsonProperty(AzureConstants.ARTIFACTORY_PRIVATE_REGISTRY)
  ARTIFACTORY_PRIVATE_REGISTRY("Artifactory Private Registry");

  private final String value;

  AzureRegistryType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
  @Override
  public String toString() {
    return value;
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static AzureRegistryType getAzureDockerRegistryTypeFromValue(@JsonProperty("type") String value) {
    for (AzureRegistryType sourceType : AzureRegistryType.values()) {
      if (sourceType.value.equalsIgnoreCase(value)) {
        return sourceType;
      }
    }
    return null;
  }
}

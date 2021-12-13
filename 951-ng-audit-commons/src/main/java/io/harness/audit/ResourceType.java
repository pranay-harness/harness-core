package io.harness.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResourceType {
  @JsonProperty("ORGANIZATION") ORGANIZATION,
  @JsonProperty("PROJECT") PROJECT,
  @JsonProperty("USER_GROUP") USER_GROUP,
  @JsonProperty("SECRET") SECRET,
  @JsonProperty("RESOURCE_GROUP") RESOURCE_GROUP,
  @JsonProperty("USER") USER,
  @JsonProperty("ROLE") ROLE,
  @JsonProperty("ROLE_ASSIGNMENT") ROLE_ASSIGNMENT,
  @JsonProperty("PIPELINE") PIPELINE,
  @JsonProperty("TRIGGER") TRIGGER,
  @JsonProperty("TEMPLATE") TEMPLATE,
  @JsonProperty("INPUT_SET") INPUT_SET,
  @JsonProperty("DELEGATE_CONFIGURATION") DELEGATE_CONFIGURATION,
  @JsonProperty("SERVICE") SERVICE,
  @JsonProperty("ENVIRONMENT") ENVIRONMENT,
  @JsonProperty("DELEGATE") DELEGATE,
  @JsonProperty("SERVICE_ACCOUNT") SERVICE_ACCOUNT,
  @JsonProperty("CONNECTOR") CONNECTOR,
  @JsonProperty("API_KEY") API_KEY,
  @JsonProperty("TOKEN") TOKEN,
  @JsonProperty("DELEGATE_TOKEN") DELEGATE_TOKEN;

  @JsonCreator
  public static ResourceType getFromString(String resourceType) {
    for (ResourceType type : ResourceType.values()) {
      if (type.name().equalsIgnoreCase(resourceType)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid Resource.Type " + resourceType);
  }
}

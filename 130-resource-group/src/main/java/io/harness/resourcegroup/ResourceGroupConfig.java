package io.harness.resourcegroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceGroupConfig {
  @JsonProperty("ng-manager") ServiceConfig ngManager;
  @JsonProperty("manager") ServiceConfig manager;

  @Value
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class ServiceConfig {
    String baseUrl;
    String secret;
  }
}

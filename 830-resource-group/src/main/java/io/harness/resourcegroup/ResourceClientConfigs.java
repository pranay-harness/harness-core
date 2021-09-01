package io.harness.resourcegroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceClientConfigs {
  @JsonProperty("ng-manager") ServiceConfig ngManager;
  @JsonProperty("manager") ServiceConfig manager;
  @JsonProperty("pipeline-service") ServiceConfig pipelineService;
  @JsonProperty("resourceGroup") ServiceConfig resourceGroupService;

  @Value
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class ServiceConfig {
    String baseUrl;
    String secret;
  }
}

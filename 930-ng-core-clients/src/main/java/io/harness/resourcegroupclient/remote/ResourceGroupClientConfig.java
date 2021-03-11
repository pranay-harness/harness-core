package io.harness.resourcegroupclient.remote;

import io.harness.remote.client.ServiceHttpClientConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceGroupClientConfig {
  @JsonProperty("serviceConfig") ServiceHttpClientConfig serviceConfig;
  @JsonProperty("secret") String secret;
}

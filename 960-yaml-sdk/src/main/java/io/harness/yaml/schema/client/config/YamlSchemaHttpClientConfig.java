package io.harness.yaml.schema.client.config;

import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
@Builder
public class YamlSchemaHttpClientConfig {
  ServiceHttpClientConfig serviceHttpClientConfig;
  String secret;
}

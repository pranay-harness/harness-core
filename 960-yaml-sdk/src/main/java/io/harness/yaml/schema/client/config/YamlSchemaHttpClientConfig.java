package io.harness.yaml.schema.client.config;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
@Builder
@OwnedBy(DX)
public class YamlSchemaHttpClientConfig {
  ServiceHttpClientConfig serviceHttpClientConfig;
  String secret;
}

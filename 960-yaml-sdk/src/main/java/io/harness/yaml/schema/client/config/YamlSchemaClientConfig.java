package io.harness.yaml.schema.client.config;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secret.ConfigSecret;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class YamlSchemaClientConfig {
  @ConfigSecret Map<String, YamlSchemaHttpClientConfig> yamlSchemaHttpClientMap;
}

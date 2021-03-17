package io.harness.yaml.schema.client.config;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class YamlSchemaClientConfig {
  Map<String, YamlSchemaHttpClientConfig> yamlSchemaHttpClientMap;
}

package io.harness.yaml;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class YamlSdkConfiguration {
  boolean requireSchemaInit;
  boolean requireSnippetInit;
  boolean requireValidatorInit;
}

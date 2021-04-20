package io.harness.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class YamlSdkConfiguration {
  boolean requireSchemaInit;
  boolean requireSnippetInit;
  boolean requireValidatorInit;
}

package io.harness.yaml.schema.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.DX)
public class StringFieldTypeMetadata implements FieldTypesMetadata {
  String pattern;
  int minLength;
}

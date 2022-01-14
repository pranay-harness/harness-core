package io.harness.secretmanagerclient;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "NGMetadataKeys")
public abstract class NGMetadata {
  private String identifier;
}

package io.harness.secretmanagerclient;

import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@FieldNameConstants(innerTypeName = "NGSecretManagerMetadataKeys")
public class NGSecretManagerMetadata extends NGMetadata {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private List<NGTag> tags;
  private String description;
}

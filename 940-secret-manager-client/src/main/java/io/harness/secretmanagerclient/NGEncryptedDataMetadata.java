package io.harness.secretmanagerclient;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@FieldNameConstants(innerTypeName = "NGEncryptedDataMetadataKeys")
public class NGEncryptedDataMetadata extends NGMetadata {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private List<String> tags;
  private String secretManagerIdentifier;
  private String secretManagerName;
  private String description;
  private boolean draft;
}

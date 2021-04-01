package io.harness.accesscontrol.acl.models;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "SourceMetadataKeys")
public class SourceMetadata {
  String userGroupIdentifier;
  String roleIdentifier;
  String roleAssignmentIdentifier;
  String resourceGroupIdentifier;
}

package io.harness.accesscontrol.acl.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "SourceMetadataKeys")
@OwnedBy(HarnessTeam.PL)
public class SourceMetadata {
  String userGroupIdentifier;
  String roleIdentifier;
  String roleAssignmentIdentifier;
  String resourceGroupIdentifier;
}

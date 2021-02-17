package io.harness.accesscontrol.acl.models;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SourceMetadata {
  String userGroupIdentifier;
  String roleIdentifier;
  String roleAssignmentIdentifier;
}

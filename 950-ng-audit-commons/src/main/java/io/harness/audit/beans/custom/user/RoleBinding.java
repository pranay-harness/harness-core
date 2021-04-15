package io.harness.audit.beans.custom.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "RoleBindingKeys")
@Builder
@OwnedBy(PL)
public class RoleBinding {
  String roleIdentifier;
  String resourceGroupIdentifier;
}

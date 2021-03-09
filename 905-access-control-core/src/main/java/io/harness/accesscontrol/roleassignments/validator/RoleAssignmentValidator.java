package io.harness.accesscontrol.roleassignments.validator;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.scopes.core.Scope;

public interface RoleAssignmentValidator {
  void validate(RoleAssignment roleAssignment, Scope scope);
}

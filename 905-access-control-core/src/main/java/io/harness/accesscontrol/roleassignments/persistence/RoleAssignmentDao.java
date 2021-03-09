package io.harness.accesscontrol.roleassignments.persistence;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleAssignmentDao {
  RoleAssignment create(@Valid RoleAssignment roleAssignment);

  PageResponse<RoleAssignment> list(
      @NotNull PageRequest pageRequest, @Valid @NotNull RoleAssignmentFilter roleAssignmentFilter);

  Optional<RoleAssignment> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  List<RoleAssignment> get(@NotEmpty String principal, @NotNull PrincipalType principalType);

  Optional<RoleAssignment> delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  long deleteMany(@Valid @NotNull RoleAssignmentFilter roleAssignmentFilter);
}
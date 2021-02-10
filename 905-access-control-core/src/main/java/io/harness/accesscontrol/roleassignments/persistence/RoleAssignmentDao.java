package io.harness.accesscontrol.roleassignments.persistence;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleAssignmentDao {
  RoleAssignment create(@Valid RoleAssignment roleAssignment);

  PageResponse<RoleAssignment> getAll(@NotNull PageRequest pageRequest, @NotEmpty String parentIdentifier,
      String principalIdentifier, String roleIdentifier);

  Optional<RoleAssignment> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  Optional<RoleAssignment> delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);
}
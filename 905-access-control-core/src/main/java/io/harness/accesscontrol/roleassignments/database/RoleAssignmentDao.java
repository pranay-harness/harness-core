package io.harness.accesscontrol.roleassignments.database;

import io.harness.accesscontrol.roleassignments.RoleAssignmentDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleAssignmentDao {
  RoleAssignmentDTO create(@Valid RoleAssignmentDTO roleAssignmentDTO);

  PageResponse<RoleAssignmentDTO> getAll(@NotNull PageRequest pageRequest, String parentIdentifier,
      String principalIdentifier, String roleIdentifier, boolean includeInheritedAssignments);

  Optional<RoleAssignmentDTO> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  RoleAssignmentDTO delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);
}
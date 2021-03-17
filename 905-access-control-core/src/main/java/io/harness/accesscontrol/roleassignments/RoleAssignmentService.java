package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidationRequest;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidationResult;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleAssignmentService {
  List<RoleAssignment> createMulti(@Valid @NotNull List<RoleAssignment> roleAssignments);

  RoleAssignment create(@Valid @NotNull RoleAssignment roleAssignment);

  PageResponse<RoleAssignment> list(
      @NotNull PageRequest pageRequest, @Valid @NotNull RoleAssignmentFilter roleAssignmentFilter);

  Optional<RoleAssignment> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  RoleAssignment update(@NotNull @Valid RoleAssignment roleAssignment);

  Optional<RoleAssignment> delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  long deleteMulti(@Valid @NotNull RoleAssignmentFilter roleAssignmentFilter);

  RoleAssignmentValidationResult validate(@Valid @NotNull RoleAssignmentValidationRequest validationRequest);
}

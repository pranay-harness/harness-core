package io.harness.accesscontrol.roleassignments;

import static lombok.AccessLevel.PRIVATE;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDao;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidator;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@ValidateOnExecution
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class RoleAssignmentServiceImpl implements RoleAssignmentService {
  ScopeService scopeService;
  RoleAssignmentDao roleAssignmentDao;
  RoleAssignmentValidator roleAssignmentValidator;

  @Override
  public RoleAssignment create(RoleAssignment roleAssignment) {
    Scope scope = scopeService.buildScopeFromScopeIdentifier(roleAssignment.getScopeIdentifier());
    roleAssignmentValidator.validate(roleAssignment, scope);
    return roleAssignmentDao.create(roleAssignment);
  }

  @Override
  public PageResponse<RoleAssignment> list(PageRequest pageRequest, RoleAssignmentFilter roleAssignmentFilter) {
    return roleAssignmentDao.list(pageRequest, roleAssignmentFilter);
  }

  @Override
  public Optional<RoleAssignment> get(String identifier, String parentIdentifier) {
    return roleAssignmentDao.get(identifier, parentIdentifier);
  }

  @Override
  public List<RoleAssignment> get(String principal, PrincipalType principalType) {
    return roleAssignmentDao.get(principal, principalType);
  }

  @Override
  public Optional<RoleAssignment> delete(String identifier, String parentIdentifier) {
    return roleAssignmentDao.delete(identifier, parentIdentifier);
  }

  @Override
  public long deleteMany(RoleAssignmentFilter roleAssignmentFilter) {
    return roleAssignmentDao.deleteMany(roleAssignmentFilter);
  }
}

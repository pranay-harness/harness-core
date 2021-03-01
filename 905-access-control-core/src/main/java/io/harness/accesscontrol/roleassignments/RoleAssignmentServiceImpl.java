package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDao;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class RoleAssignmentServiceImpl implements RoleAssignmentService {
  private final RoleAssignmentDao roleAssignmentDao;

  @Inject
  public RoleAssignmentServiceImpl(RoleAssignmentDao roleAssignmentDao) {
    this.roleAssignmentDao = roleAssignmentDao;
  }

  @Override
  public RoleAssignment create(RoleAssignment roleAssignment) {
    return roleAssignmentDao.create(roleAssignment);
  }

  @Override
  public PageResponse<RoleAssignment> list(
      PageRequest pageRequest, String parentIdentifier, RoleAssignmentFilter roleAssignmentFilter) {
    return roleAssignmentDao.list(pageRequest, parentIdentifier, roleAssignmentFilter);
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
}

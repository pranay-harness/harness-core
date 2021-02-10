package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDao;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
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
    roleAssignment.setDisabled(false);
    roleAssignment.setManaged(false);
    return roleAssignmentDao.create(roleAssignment);
  }

  @Override
  public PageResponse<RoleAssignment> getAll(
      PageRequest pageRequest, String parentIdentifier, String principalIdentifier, String roleIdentifier) {
    return roleAssignmentDao.getAll(pageRequest, parentIdentifier, principalIdentifier, roleIdentifier);
  }

  @Override
  public Optional<RoleAssignment> get(String identifier, String parentIdentifier) {
    return roleAssignmentDao.get(identifier, parentIdentifier);
  }

  @Override
  public Optional<RoleAssignment> delete(String identifier, String parentIdentifier) {
    return roleAssignmentDao.delete(identifier, parentIdentifier);
  }
}

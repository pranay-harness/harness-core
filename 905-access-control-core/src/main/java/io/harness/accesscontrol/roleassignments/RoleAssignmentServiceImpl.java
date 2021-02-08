package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.roleassignments.database.RoleAssignmentDao;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
import java.util.Optional;

public class RoleAssignmentServiceImpl implements RoleAssignmentService {
  private final RoleAssignmentDao roleAssignmentDao;

  @Inject
  public RoleAssignmentServiceImpl(RoleAssignmentDao roleAssignmentDao) {
    this.roleAssignmentDao = roleAssignmentDao;
  }

  @Override
  public RoleAssignmentDTO create(RoleAssignmentDTO roleAssignmentDTO) {
    return roleAssignmentDao.create(roleAssignmentDTO);
  }

  @Override
  public PageResponse<RoleAssignmentDTO> getAll(PageRequest pageRequest, String parentIdentifier,
      String principalIdentifier, String roleIdentifier, boolean includeInheritedAssignments) {
    return roleAssignmentDao.getAll(
        pageRequest, parentIdentifier, principalIdentifier, roleIdentifier, includeInheritedAssignments);
  }

  @Override
  public Optional<RoleAssignmentDTO> get(String identifier, String parentIdentifier) {
    return roleAssignmentDao.get(identifier, parentIdentifier);
  }

  @Override
  public RoleAssignmentDTO delete(String identifier, String parentIdentifier) {
    return roleAssignmentDao.delete(identifier, parentIdentifier);
  }
}

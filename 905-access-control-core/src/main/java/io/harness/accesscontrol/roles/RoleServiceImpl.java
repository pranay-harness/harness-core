package io.harness.accesscontrol.roles;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roles.persistence.RoleDao;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
import java.util.Optional;
import javax.ws.rs.BadRequestException;

public class RoleServiceImpl implements RoleService {
  private final RoleDao roleDao;

  @Inject
  public RoleServiceImpl(RoleDao roleDao) {
    this.roleDao = roleDao;
  }

  @Override
  public Role create(Role role) {
    role.setManaged(false);
    return roleDao.create(role);
  }

  @Override
  public PageResponse<Role> getAll(PageRequest pageRequest, String parentIdentifier, boolean includeManaged) {
    if (isEmpty(parentIdentifier) && !includeManaged) {
      throw new BadRequestException("Either includeManaged should be true, or parentIdentifier should be non-empty");
    }
    return roleDao.getAll(pageRequest, parentIdentifier, includeManaged);
  }

  @Override
  public Optional<Role> get(String identifier, String parentIdentifier) {
    return roleDao.get(identifier, parentIdentifier);
  }

  @Override
  public Role update(Role role) {
    return null;
  }

  @Override
  public Optional<Role> delete(String identifier, String parentIdentifier) {
    return roleDao.delete(identifier, parentIdentifier);
  }
}

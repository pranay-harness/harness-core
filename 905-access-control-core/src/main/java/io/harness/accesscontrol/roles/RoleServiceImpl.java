package io.harness.accesscontrol.roles;

import io.harness.accesscontrol.roles.database.RoleDao;

import com.google.inject.Inject;
import java.util.Optional;

public class RoleServiceImpl implements RoleService {
  private final RoleDao roleDao;

  @Inject
  public RoleServiceImpl(RoleDao roleDao) {
    this.roleDao = roleDao;
  }

  @Override
  public String create(RoleDTO roleDTO) {
    return roleDao.create(roleDTO);
  }

  @Override
  public Optional<RoleDTO> get(String identifier, String parentIdentifier) {
    return roleDao.get(identifier, parentIdentifier);
  }

  @Override
  public String update(RoleDTO roleDTO) {
    return null;
  }

  @Override
  public RoleDTO delete(String identifier, String parentIdentifier) {
    return null;
  }
}

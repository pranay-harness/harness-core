package io.harness.accesscontrol.rolebindings;

import io.harness.accesscontrol.rolebindings.database.RoleBindingDao;

import com.google.inject.Inject;
import java.util.Optional;

public class RoleBindingServiceImpl implements RoleBindingService {
  private final RoleBindingDao roleBindingDao;

  @Inject
  public RoleBindingServiceImpl(RoleBindingDao roleBindingDao) {
    this.roleBindingDao = roleBindingDao;
  }

  @Override
  public String create(RoleBindingDTO roleBindingDTO) {
    return roleBindingDao.create(roleBindingDTO);
  }

  @Override
  public Optional<RoleBindingDTO> get(String identifier, String parentIdentifier) {
    return roleBindingDao.get(identifier, parentIdentifier);
  }

  @Override
  public String update(RoleBindingDTO roleBindingDTO) {
    return roleBindingDao.update(roleBindingDTO);
  }

  @Override
  public void delete(String identifier, String parentIdentifier) {
    roleBindingDao.delete(identifier, parentIdentifier);
  }
}

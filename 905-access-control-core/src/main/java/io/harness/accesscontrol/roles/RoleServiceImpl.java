package io.harness.accesscontrol.roles;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roles.database.RoleDao;
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
  public RoleDTO create(RoleDTO roleDTO) {
    return roleDao.create(roleDTO);
  }

  @Override
  public PageResponse<RoleDTO> getAll(PageRequest pageRequest, String parentIdentifier, boolean includeDefault) {
    if (isEmpty(parentIdentifier) && !includeDefault) {
      throw new BadRequestException("Either includeDefault should be true, or parentIdentifier should be non-empty");
    }
    return roleDao.getAll(pageRequest, parentIdentifier, includeDefault);
  }

  @Override
  public Optional<RoleDTO> get(String identifier, String parentIdentifier) {
    return roleDao.get(identifier, parentIdentifier);
  }

  @Override
  public RoleDTO update(RoleDTO roleDTO) {
    return null;
  }

  @Override
  public RoleDTO delete(String identifier, String parentIdentifier) {
    return roleDao.delete(identifier, parentIdentifier);
  }
}

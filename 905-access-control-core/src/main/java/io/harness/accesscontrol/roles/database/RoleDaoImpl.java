package io.harness.accesscontrol.roles.database;

import io.harness.accesscontrol.roles.RoleDTO;
import io.harness.exception.DuplicateFieldException;

import com.google.inject.Inject;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;

public class RoleDaoImpl implements RoleDao {
  private final RoleRepository roleRepository;

  @Inject
  public RoleDaoImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Override
  public String create(RoleDTO roleDTO) {
    Role role = RoleMapper.toRole(roleDTO);
    try {
      return roleRepository.save(role).getIdentifier();
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(String.format("A role with identifier %s in this scope %s is already present",
          role.getIdentifier(), role.getParentIdentifier()));
    }
  }

  @Override
  public Optional<RoleDTO> get(String identifier, String parentIdentifier) {
    Optional<Role> role = roleRepository.findByIdentifierAndParentIdentifier(identifier, parentIdentifier);
    return role.flatMap(r -> Optional.of(RoleMapper.fromRole(r)));
  }

  @Override
  public String update(RoleDTO permissionDTO) {
    return null;
  }

  @Override
  public void delete(String identifier, String parentIdentifier) {}
}

package io.harness.accesscontrol.permissions.persistence;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.scopes.Scope;
import io.harness.exception.DuplicateFieldException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@ValidateOnExecution
public class PermissionDaoImpl implements PermissionDao {
  private final PermissionRepository permissionRepository;

  @Inject
  public PermissionDaoImpl(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  @Override
  public String create(Permission permissionDTO) {
    PermissionDBO permissionDBO = PermissionDBOMapper.toDBO(permissionDTO);
    try {
      return permissionRepository.save(permissionDBO).getIdentifier();
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format("A permission with identifier %s is already present", permissionDBO.getIdentifier()));
    }
  }

  @Override
  public List<Permission> list(Scope scope, String resourceType) {
    Collection<PermissionDBO> permissionDBOs = permissionRepository.findAllByScopesContaining(scope.getDBKey());
    return permissionDBOs.stream().map(PermissionDBOMapper::fromDBO).collect(Collectors.toList());
  }

  @Override
  public Optional<Permission> get(String identifier) {
    Optional<PermissionDBO> permission = permissionRepository.findByIdentifier(identifier);
    return permission.flatMap(p -> Optional.of(PermissionDBOMapper.fromDBO(p)));
  }

  @Override
  public String update(Permission permission) {
    return null;
  }

  @Override
  public void delete(String identifier) {
    permissionRepository.deleteByIdentifier(identifier);
  }
}

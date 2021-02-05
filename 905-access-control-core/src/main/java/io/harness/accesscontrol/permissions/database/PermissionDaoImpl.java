package io.harness.accesscontrol.permissions.database;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.permissions.PermissionDTO;
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
  public String create(PermissionDTO permissionDTO) {
    Permission permission = PermissionMapper.toPermission(permissionDTO);
    try {
      return permissionRepository.save(permission).getIdentifier();
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format("A permission with identifier %s is already present", permission.getIdentifier()));
    }
  }

  @Override
  public List<PermissionDTO> list(Scope scope, String resourceType) {
    Collection<Permission> permissions;
    if (isNotEmpty(resourceType)) {
      permissions = permissionRepository.findAllByScopesContainingAndResourceType(scope.getDBKey(), resourceType);
    } else {
      permissions = permissionRepository.findAllByScopesContaining(scope.getDBKey());
    }
    return permissions.stream().map(PermissionMapper::fromPermission).collect(Collectors.toList());
  }

  @Override
  public Optional<PermissionDTO> get(String identifier) {
    Optional<Permission> permission = permissionRepository.findByIdentifier(identifier);
    return permission.flatMap(p -> Optional.of(PermissionMapper.fromPermission(p)));
  }

  @Override
  public String update(PermissionDTO permissionDTO) {
    return null;
  }

  @Override
  public void delete(String identifier) {
    permissionRepository.deleteByIdentifier(identifier);
  }
}

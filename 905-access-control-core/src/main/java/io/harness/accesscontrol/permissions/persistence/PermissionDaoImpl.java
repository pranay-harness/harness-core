package io.harness.accesscontrol.permissions.persistence;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.persistence.PermissionDBO.PermissionDBOKeys;
import io.harness.accesscontrol.permissions.persistence.repositories.PermissionRepository;
import io.harness.exception.DuplicateFieldException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@ValidateOnExecution
public class PermissionDaoImpl implements PermissionDao {
  private final PermissionRepository permissionRepository;

  @Inject
  public PermissionDaoImpl(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  @Override
  public Permission create(Permission permissionDTO) {
    PermissionDBO permissionDBO = PermissionDBOMapper.toDBO(permissionDTO);
    try {
      return PermissionDBOMapper.fromDBO(permissionRepository.save(permissionDBO));
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format("A permission with identifier %s is already present", permissionDBO.getIdentifier()));
    }
  }

  @Override
  public List<Permission> list(PermissionFilter permissionFilter) {
    if (permissionFilter.isEmpty()) {
      Iterable<PermissionDBO> permissionDBOIterable = permissionRepository.findAll();
      List<Permission> permissions = new ArrayList<>();
      permissionDBOIterable.iterator().forEachRemaining(
          permissionDBO -> permissions.add(PermissionDBOMapper.fromDBO(permissionDBO)));
      return permissions;
    }

    Criteria criteria = new Criteria();
    if (!permissionFilter.getIdentifierFilter().isEmpty()) {
      criteria.and(PermissionDBOKeys.identifier).in(permissionFilter.getIdentifierFilter());
    }
    if (!permissionFilter.getAllowedScopeLevelsFilter().isEmpty()) {
      criteria.and(PermissionDBOKeys.allowedScopeLevels).in(permissionFilter.getAllowedScopeLevelsFilter());
    }
    if (!permissionFilter.getStatusFilter().isEmpty()) {
      criteria.and(PermissionDBOKeys.status).in(permissionFilter.getStatusFilter());
    }
    List<PermissionDBO> permissionDBOList = permissionRepository.findAll(criteria);
    return permissionDBOList.stream().map(PermissionDBOMapper::fromDBO).collect(Collectors.toList());
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

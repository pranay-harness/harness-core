package io.harness.accesscontrol.roles.persistence;

import static io.harness.accesscontrol.roles.persistence.RoleDBOMapper.fromDBO;
import static io.harness.accesscontrol.roles.persistence.RoleDBOMapper.toDBO;

import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.persistence.RoleDBO.RoleDBOKeys;
import io.harness.accesscontrol.roles.persistence.repositories.RoleRepository;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@ValidateOnExecution
public class RoleDaoImpl implements RoleDao {
  private final RoleRepository roleRepository;

  @Inject
  public RoleDaoImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Override
  public Role create(Role role) {
    RoleDBO roleDBO = toDBO(role);
    try {
      return fromDBO(roleRepository.save(roleDBO));
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(String.format("A role with identifier %s in this scope %s is already present",
          roleDBO.getIdentifier(), roleDBO.getScopeIdentifier()));
    }
  }

  @Override
  public PageResponse<Role> list(
      PageRequest pageRequest, String scopeIdentifier, boolean includeManaged, Set<String> allowedScopeLevels) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = new Criteria();
    if (includeManaged) {
      criteria.orOperator(Criteria.where(RoleDBOKeys.scopeIdentifier).is(scopeIdentifier),
          Criteria.where(RoleDBOKeys.managed).is(true));
    } else {
      criteria.and(RoleDBOKeys.scopeIdentifier).is(scopeIdentifier);
    }
    if (!allowedScopeLevels.isEmpty()) {
      criteria.and(RoleDBOKeys.allowedScopeLevels).in(allowedScopeLevels);
    }
    Page<RoleDBO> rolePages = roleRepository.findAll(criteria, pageable);
    return PageUtils.getNGPageResponse(rolePages.map(RoleDBOMapper::fromDBO));
  }

  @Override
  public Optional<Role> get(String identifier, String scopeIdentifier, boolean isManaged) {
    return getInternal(identifier, scopeIdentifier, isManaged).flatMap(r -> Optional.of(fromDBO(r)));
  }

  private Optional<RoleDBO> getInternal(String identifier, String scopeIdentifier, boolean isManaged) {
    Criteria criteria = new Criteria();
    if (isManaged) {
      criteria.orOperator(Criteria.where(RoleDBOKeys.scopeIdentifier).is(scopeIdentifier),
          Criteria.where(RoleDBOKeys.managed).is(true));
    } else {
      criteria.and(RoleDBOKeys.scopeIdentifier).is(scopeIdentifier);
    }
    criteria.and(RoleDBOKeys.identifier).is(identifier);
    return roleRepository.find(criteria);
  }

  @Override
  public Role update(Role roleUpdate) {
    Optional<RoleDBO> roleDBOOptional =
        getInternal(roleUpdate.getIdentifier(), roleUpdate.getScopeIdentifier(), roleUpdate.isManaged());
    if (roleDBOOptional.isPresent()) {
      RoleDBO roleUpdateDBO = toDBO(roleUpdate);
      roleUpdateDBO.setId(roleDBOOptional.get().getId());
      return fromDBO(roleRepository.save(roleUpdateDBO));
    }
    throw new InvalidRequestException(
        String.format("Could not find the role in the scope %s", roleUpdate.getScopeIdentifier()));
  }

  @Override
  public Optional<Role> delete(String identifier, String scopeIdentifier) {
    return roleRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .stream()
        .findFirst()
        .flatMap(r -> Optional.of(fromDBO(r)));
  }

  @Override
  public boolean removePermissionFromRoles(String permissionIdentifier) {
    Criteria criteria = new Criteria();
    criteria.and(RoleDBOKeys.permissions).is(permissionIdentifier);
    Update update = new Update().pull(RoleDBOKeys.permissions, permissionIdentifier);
    UpdateResult updateResult = roleRepository.updateMulti(criteria, update);
    return updateResult.getMatchedCount() == updateResult.getModifiedCount();
  }
}

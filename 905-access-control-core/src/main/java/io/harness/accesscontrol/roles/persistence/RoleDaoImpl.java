package io.harness.accesscontrol.roles.persistence;

import static io.harness.accesscontrol.roles.persistence.RoleDBOMapper.fromDBO;
import static io.harness.accesscontrol.roles.persistence.RoleDBOMapper.toDBO;

import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.persistence.RoleDBO.RoleKeys;
import io.harness.accesscontrol.roles.persistence.repositories.RoleRepository;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

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
  public PageResponse<Role> getAll(PageRequest pageRequest, String parentIdentifier, boolean includeManaged) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = new Criteria();
    criteria.orOperator(Criteria.where(RoleKeys.scopeIdentifier).is(parentIdentifier),
        Criteria.where(RoleKeys.managed).is(includeManaged));
    Page<RoleDBO> rolePages = roleRepository.findAll(criteria, pageable);
    return PageUtils.getNGPageResponse(rolePages.map(RoleDBOMapper::fromDBO));
  }

  @Override
  public Optional<Role> get(String identifier, String parentIdentifier) {
    Optional<RoleDBO> role = roleRepository.findByIdentifierAndScopeIdentifier(identifier, parentIdentifier);
    return role.flatMap(r -> Optional.of(fromDBO(r)));
  }

  @Override
  public Role update(Role role) {
    return null;
  }

  @Override
  public Optional<Role> delete(String identifier, String parentIdentifier) {
    return roleRepository.deleteByIdentifierAndScopeIdentifier(identifier, parentIdentifier)
        .stream()
        .findFirst()
        .flatMap(r -> Optional.of(fromDBO(r)));
  }
}

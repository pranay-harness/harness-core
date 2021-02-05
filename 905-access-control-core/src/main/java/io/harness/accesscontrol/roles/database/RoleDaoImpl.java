package io.harness.accesscontrol.roles.database;

import io.harness.accesscontrol.roles.RoleDTO;
import io.harness.accesscontrol.roles.database.Role.RoleKeys;
import io.harness.accesscontrol.roles.database.repository.RoleRepository;
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
  private final io.harness.accesscontrol.roles.database.repository.RoleRepository roleRepository;

  @Inject
  public RoleDaoImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Override
  public RoleDTO create(RoleDTO roleDTO) {
    Role role = RoleMapper.toRole(roleDTO);
    try {
      return RoleMapper.fromRole(roleRepository.save(role));
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(String.format("A role with identifier %s in this scope %s is already present",
          role.getIdentifier(), role.getParentIdentifier()));
    }
  }

  @Override
  public PageResponse<RoleDTO> getAll(PageRequest pageRequest, String parentIdentifier, boolean includeDefault) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = new Criteria();
    criteria.orOperator(Criteria.where(RoleKeys.parentIdentifier).is(parentIdentifier),
        Criteria.where(RoleKeys.isDefault).is(includeDefault));
    Page<Role> rolePages = roleRepository.findAll(criteria, pageable);
    return PageUtils.getNGPageResponse(rolePages.map(RoleMapper::fromRole));
  }

  @Override
  public Optional<RoleDTO> get(String identifier, String parentIdentifier) {
    Optional<Role> role = roleRepository.findByIdentifierAndParentIdentifier(identifier, parentIdentifier);
    return role.flatMap(r -> Optional.of(RoleMapper.fromRole(r)));
  }

  @Override
  public RoleDTO update(RoleDTO roleDTO) {
    return null;
  }

  @Override
  public RoleDTO delete(String identifier, String parentIdentifier) {
    Role role = roleRepository.deleteByIdentifierAndParentIdentifier(identifier, parentIdentifier);
    return RoleMapper.fromRole(role);
  }
}

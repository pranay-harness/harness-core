package io.harness.accesscontrol.roleassignments.persistence;

import static io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBOMapper.fromDBO;
import static io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBOMapper.toDBO;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@ValidateOnExecution
public class RoleAssignmentDaoImpl implements RoleAssignmentDao {
  private final RoleAssignmentRepository roleAssignmentRepository;

  @Inject
  public RoleAssignmentDaoImpl(RoleAssignmentRepository roleAssignmentRepository) {
    this.roleAssignmentRepository = roleAssignmentRepository;
  }

  @Override
  public RoleAssignment create(RoleAssignment roleAssignment) {
    RoleAssignmentDBO roleAssignmentDBO = toDBO(roleAssignment);
    try {
      return fromDBO(roleAssignmentRepository.save(roleAssignmentDBO));
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format("A role assignment with identifier %s in this scope %s is already present",
              roleAssignmentDBO.getIdentifier(), roleAssignmentDBO.getScopeIdentifier()));
    }
  }

  @Override
  public PageResponse<RoleAssignment> list(
      PageRequest pageRequest, String scopeIdentifier, RoleAssignmentFilter roleAssignmentFilter) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = new Criteria();
    criteria.and(RoleAssignmentDBOKeys.scopeIdentifier).is(scopeIdentifier);

    if (!roleAssignmentFilter.getRoleFilter().isEmpty()) {
      criteria.and(RoleAssignmentDBOKeys.roleIdentifier).in(roleAssignmentFilter.getRoleFilter());
    }

    if (!roleAssignmentFilter.getResourceGroupFilter().isEmpty()) {
      criteria.and(RoleAssignmentDBOKeys.resourceGroupIdentifier).in(roleAssignmentFilter.getResourceGroupFilter());
    }

    if (!roleAssignmentFilter.getManagedFilter().isEmpty()) {
      criteria.and(RoleAssignmentDBOKeys.managed).in(roleAssignmentFilter.getManagedFilter());
    }

    if (!roleAssignmentFilter.getDisabledFilter().isEmpty()) {
      criteria.and(RoleAssignmentDBOKeys.disabled).in(roleAssignmentFilter.getDisabledFilter());
    }

    if (!roleAssignmentFilter.getPrincipalTypeFilter().isEmpty()) {
      criteria.and(RoleAssignmentDBOKeys.principalType).in(roleAssignmentFilter.getPrincipalTypeFilter());
    }

    else if (!roleAssignmentFilter.getPrincipalFilter().isEmpty()) {
      criteria.orOperator(roleAssignmentFilter.getPrincipalFilter()
                              .stream()
                              .map(principal
                                  -> Criteria.where(RoleAssignmentDBOKeys.principalIdentifier)
                                         .is(principal.getPrincipalIdentifier())
                                         .and(RoleAssignmentDBOKeys.principalType)
                                         .is(principal.getPrincipalType()))
                              .toArray(Criteria[] ::new));
    }

    Page<RoleAssignmentDBO> assignmentPage = roleAssignmentRepository.findAll(criteria, pageable);
    return PageUtils.getNGPageResponse(assignmentPage.map(RoleAssignmentDBOMapper::fromDBO));
  }

  @Override
  public Optional<RoleAssignment> get(String identifier, String parentIdentifier) {
    Optional<RoleAssignmentDBO> roleAssignment =
        roleAssignmentRepository.findByIdentifierAndScopeIdentifier(identifier, parentIdentifier);
    return roleAssignment.flatMap(r -> Optional.of(RoleAssignmentDBOMapper.fromDBO(r)));
  }

  @Override
  public List<RoleAssignment> get(String principal, PrincipalType principalType) {
    return roleAssignmentRepository.findByPrincipalIdentifierAndPrincipalType(principal, principalType)
        .stream()
        .map(RoleAssignmentDBOMapper::fromDBO)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<RoleAssignment> delete(String identifier, String parentIdentifier) {
    return roleAssignmentRepository.deleteByIdentifierAndScopeIdentifier(identifier, parentIdentifier)
        .stream()
        .findFirst()
        .flatMap(r -> Optional.of(RoleAssignmentDBOMapper.fromDBO(r)));
  }
}

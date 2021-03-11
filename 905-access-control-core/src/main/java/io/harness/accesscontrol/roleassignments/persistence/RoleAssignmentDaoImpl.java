package io.harness.accesscontrol.roleassignments.persistence;

import static io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBOMapper.fromDBO;
import static io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBOMapper.toDBO;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.regex.Pattern;
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
  public PageResponse<RoleAssignment> list(PageRequest pageRequest, RoleAssignmentFilter roleAssignmentFilter) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = createCriteriaFromFilter(roleAssignmentFilter);
    Page<RoleAssignmentDBO> assignmentPage = roleAssignmentRepository.findAll(criteria, pageable);
    return PageUtils.getNGPageResponse(assignmentPage.map(RoleAssignmentDBOMapper::fromDBO));
  }

  @Override
  public Optional<RoleAssignment> get(String identifier, String scopeIdentifier) {
    Optional<RoleAssignmentDBO> roleAssignment =
        roleAssignmentRepository.findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier);
    return roleAssignment.flatMap(r -> Optional.of(RoleAssignmentDBOMapper.fromDBO(r)));
  }

  @Override
  public RoleAssignment update(RoleAssignment roleAssignmentUpdate) {
    Optional<RoleAssignmentDBO> roleAssignmentDBOOptional = roleAssignmentRepository.findByIdentifierAndScopeIdentifier(
        roleAssignmentUpdate.getIdentifier(), roleAssignmentUpdate.getScopeIdentifier());
    if (roleAssignmentDBOOptional.isPresent()) {
      RoleAssignmentDBO roleAssignmentUpdateDBO = toDBO(roleAssignmentUpdate);
      roleAssignmentUpdateDBO.setId(roleAssignmentDBOOptional.get().getId());
      roleAssignmentUpdateDBO.setCreatedAt(roleAssignmentDBOOptional.get().getCreatedAt());
      roleAssignmentUpdateDBO.setLastModifiedAt(roleAssignmentDBOOptional.get().getLastModifiedAt());
      return fromDBO(roleAssignmentRepository.save(roleAssignmentUpdateDBO));
    }
    throw new InvalidRequestException(
        String.format("Could not find the role assignment in the scope %s", roleAssignmentUpdate.getScopeIdentifier()));
  }

  @Override
  public Optional<RoleAssignment> delete(String identifier, String scopeIdentifier) {
    return roleAssignmentRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .stream()
        .findFirst()
        .flatMap(r -> Optional.of(RoleAssignmentDBOMapper.fromDBO(r)));
  }

  @Override
  public long deleteMulti(RoleAssignmentFilter roleAssignmentFilter) {
    return roleAssignmentRepository.deleteMulti(createCriteriaFromFilter(roleAssignmentFilter));
  }

  private Criteria createCriteriaFromFilter(RoleAssignmentFilter roleAssignmentFilter) {
    Criteria criteria = new Criteria();
    if (!roleAssignmentFilter.isIncludeChildScopes()) {
      criteria.and(RoleAssignmentDBOKeys.scopeIdentifier).is(roleAssignmentFilter.getScopeFilter());
    } else {
      Pattern startsWithScope = Pattern.compile("^".concat(roleAssignmentFilter.getScopeFilter()));
      criteria.and(RoleAssignmentDBOKeys.scopeIdentifier).regex(startsWithScope);
    }

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
    return criteria;
  }
}

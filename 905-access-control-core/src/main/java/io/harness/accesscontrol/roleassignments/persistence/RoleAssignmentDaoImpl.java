package io.harness.accesscontrol.roleassignments.persistence;

import static io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBOMapper.fromDBO;
import static io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBOMapper.toDBO;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO.RoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.Optional;
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
  public PageResponse<RoleAssignment> getAll(
      PageRequest pageRequest, String parentIdentifier, String principalIdentifier, String roleIdentifier) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria parentCriteria = Criteria.where(RoleAssignmentDBOKeys.scopeIdentifier).is(parentIdentifier);
    Criteria principalCriteria = Criteria.where(RoleAssignmentDBOKeys.principalIdentifier).is(principalIdentifier);
    Criteria roleCriteria = Criteria.where(RoleAssignmentDBOKeys.principalIdentifier).is(roleIdentifier);
    Criteria criteria;
    if (isEmpty(principalIdentifier) && isEmpty(roleIdentifier)) {
      criteria = parentCriteria;
    } else if (isEmpty(principalIdentifier)) {
      criteria = new Criteria().andOperator(parentCriteria, roleCriteria);
    } else if (isEmpty(roleIdentifier)) {
      criteria = new Criteria().andOperator(parentCriteria, principalCriteria);
    } else {
      criteria = new Criteria().andOperator(parentCriteria, new Criteria().orOperator(principalCriteria, roleCriteria));
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
  public Optional<RoleAssignment> delete(String identifier, String parentIdentifier) {
    return roleAssignmentRepository.deleteByIdentifierAndScopeIdentifier(identifier, parentIdentifier)
        .stream()
        .findFirst()
        .flatMap(r -> Optional.of(RoleAssignmentDBOMapper.fromDBO(r)));
  }
}

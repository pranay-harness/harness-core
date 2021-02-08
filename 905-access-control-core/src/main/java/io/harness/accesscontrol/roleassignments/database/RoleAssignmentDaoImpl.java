package io.harness.accesscontrol.roleassignments.database;

import io.harness.accesscontrol.roleassignments.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.database.RoleAssignment.RoleAssignmentKeys;
import io.harness.accesscontrol.roleassignments.database.repositories.RoleAssignmentRepository;
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

public class RoleAssignmentDaoImpl implements RoleAssignmentDao {
  private final RoleAssignmentRepository roleAssignmentRepository;

  @Inject
  public RoleAssignmentDaoImpl(RoleAssignmentRepository roleAssignmentRepository) {
    this.roleAssignmentRepository = roleAssignmentRepository;
  }

  @Override
  public RoleAssignmentDTO create(RoleAssignmentDTO roleAssignmentDTO) {
    RoleAssignment roleAssignment = RoleAssignmentMapper.fromDTO(roleAssignmentDTO);
    try {
      return RoleAssignmentMapper.toDTO(roleAssignmentRepository.save(roleAssignment));
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format("A role binding with identifier %s in this scope %s is already present",
              roleAssignment.getIdentifier(), roleAssignment.getParentIdentifier()));
    }
  }

  @Override
  public PageResponse<RoleAssignmentDTO> getAll(PageRequest pageRequest, String parentIdentifier,
      String principalIdentifier, String roleIdentifier, boolean includeInheritedAssignments) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Criteria criteria = new Criteria();
    criteria.andOperator(Criteria.where(RoleAssignmentKeys.parentIdentifier).is(parentIdentifier),
        criteria.orOperator(Criteria.where(RoleAssignmentKeys.principalIdentifier).is(principalIdentifier),
            Criteria.where(RoleAssignmentKeys.roleIdentifier).is(roleIdentifier)));
    Page<RoleAssignment> assignmentPage = roleAssignmentRepository.findAll(criteria, pageable);
    return PageUtils.getNGPageResponse(assignmentPage.map(RoleAssignmentMapper::toDTO));
  }

  @Override
  public Optional<RoleAssignmentDTO> get(String identifier, String parentIdentifier) {
    Optional<RoleAssignment> roleBinding =
        roleAssignmentRepository.findByIdentifierAndParentIdentifier(identifier, parentIdentifier);
    return roleBinding.flatMap(r -> Optional.of(RoleAssignmentMapper.toDTO(r)));
  }

  @Override
  public RoleAssignmentDTO delete(String identifier, String parentIdentifier) {
    return RoleAssignmentMapper.toDTO(
        roleAssignmentRepository.deleteByIdentifierAndParentIdentifier(identifier, parentIdentifier));
  }
}

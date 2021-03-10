package io.harness.accesscontrol.roleassignments;

import static lombok.AccessLevel.PRIVATE;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDao;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidator;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@ValidateOnExecution
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class RoleAssignmentServiceImpl implements RoleAssignmentService {
  ScopeService scopeService;
  RoleAssignmentDao roleAssignmentDao;
  RoleAssignmentValidator roleAssignmentValidator;
  TransactionTemplate transactionTemplate;
  private static final RetryPolicy<Object> createMultiRetryPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed to create the role assignments, the operation is rolled back; attempt: {}",
      "[Failed]: Failed to create the role assignments, the operation is rolled back; attempt: {}",
      ImmutableList.of(TransactionException.class), Duration.ofSeconds(15), 3, log);

  @Override
  public List<RoleAssignment> createMulti(List<RoleAssignment> roleAssignments) {
    return Failsafe.with(createMultiRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      List<RoleAssignment> createdRoleAssignments = new ArrayList<>();
      roleAssignments.forEach(roleAssignment -> createdRoleAssignments.add(create(roleAssignment)));
      return createdRoleAssignments;
    }));
  }

  @Override
  public RoleAssignment create(RoleAssignment roleAssignment) {
    Scope scope = scopeService.buildScopeFromScopeIdentifier(roleAssignment.getScopeIdentifier());
    roleAssignmentValidator.validate(roleAssignment, scope);
    return roleAssignmentDao.create(roleAssignment);
  }

  @Override
  public PageResponse<RoleAssignment> list(PageRequest pageRequest, RoleAssignmentFilter roleAssignmentFilter) {
    return roleAssignmentDao.list(pageRequest, roleAssignmentFilter);
  }

  @Override
  public Optional<RoleAssignment> get(String identifier, String parentIdentifier) {
    return roleAssignmentDao.get(identifier, parentIdentifier);
  }

  @Override
  public RoleAssignment update(RoleAssignment roleAssignmentUpdate) {
    Optional<RoleAssignment> currentRoleAssignmentOptional =
        get(roleAssignmentUpdate.getIdentifier(), roleAssignmentUpdate.getScopeIdentifier());
    if (currentRoleAssignmentOptional.isPresent()) {
      RoleAssignment roleAssignment = currentRoleAssignmentOptional.get();
      if (!roleAssignmentUpdate.getResourceGroupIdentifier().equals(roleAssignment.getResourceGroupIdentifier())) {
        throw new InvalidRequestException("Cannot change resource group in the role assignment");
      }
      if (!roleAssignmentUpdate.getPrincipalIdentifier().equals(roleAssignment.getPrincipalIdentifier())) {
        throw new InvalidRequestException("Cannot change principal in the role assignment");
      }
      if (!roleAssignmentUpdate.getPrincipalType().equals(roleAssignment.getPrincipalType())) {
        throw new InvalidRequestException("Cannot change principal type in the role assignment");
      }
      if (!roleAssignmentUpdate.getRoleIdentifier().equals(roleAssignment.getRoleIdentifier())) {
        throw new InvalidRequestException("Cannot change role in the role assignment");
      }
      roleAssignmentUpdate.setManaged(roleAssignment.isManaged());
      roleAssignmentUpdate.setVersion(roleAssignment.getVersion());
      return roleAssignmentDao.update(roleAssignmentUpdate);
    }
    throw new InvalidRequestException(
        String.format("Could not find the role assignment in the scope %s", roleAssignmentUpdate.getScopeIdentifier()));
  }

  @Override
  public Optional<RoleAssignment> delete(String identifier, String parentIdentifier) {
    return roleAssignmentDao.delete(identifier, parentIdentifier);
  }

  @Override
  public long deleteMulti(RoleAssignmentFilter roleAssignmentFilter) {
    return roleAssignmentDao.deleteMulti(roleAssignmentFilter);
  }
}

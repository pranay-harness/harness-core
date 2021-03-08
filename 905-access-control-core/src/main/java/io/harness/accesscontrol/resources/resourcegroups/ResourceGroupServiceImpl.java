package io.harness.accesscontrol.resources.resourcegroups;

import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDao;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Singleton
@ValidateOnExecution
public class ResourceGroupServiceImpl implements ResourceGroupService {
  private final ResourceGroupDao resourceGroupDao;
  private final RoleAssignmentService roleAssignmentService;
  private final TransactionTemplate transactionTemplate;
  private static final RetryPolicy<Object> deleteResourceGroupTransactionPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed to delete resource group and corresponding role assignments; attempt: {}",
      "[Failed]: Failed to delete resource group and corresponding role assignments; attempt: {}",
      ImmutableList.of(TransactionException.class), Duration.ofSeconds(5), 3, log);

  @Inject
  public ResourceGroupServiceImpl(ResourceGroupDao resourceGroupDao, RoleAssignmentService roleAssignmentService,
      TransactionTemplate transactionTemplate) {
    this.resourceGroupDao = resourceGroupDao;
    this.roleAssignmentService = roleAssignmentService;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public ResourceGroup upsert(ResourceGroup resourceGroup) {
    return resourceGroupDao.upsert(resourceGroup);
  }

  @Override
  public PageResponse<ResourceGroup> list(PageRequest pageRequest, String scopeIdentifier) {
    return resourceGroupDao.list(pageRequest, scopeIdentifier);
  }

  @Override
  public List<ResourceGroup> list(List<String> resourceGroupIdentifiers, String scopeIdentifier) {
    return resourceGroupDao.list(resourceGroupIdentifiers, scopeIdentifier);
  }

  @Override
  public Optional<ResourceGroup> get(String identifier, String scopeIdentifier) {
    return resourceGroupDao.get(identifier, scopeIdentifier);
  }

  @Override
  public ResourceGroup delete(String identifier, String scopeIdentifier) {
    Optional<ResourceGroup> currentResourceGroupOptional = get(identifier, scopeIdentifier);
    if (!currentResourceGroupOptional.isPresent()) {
      throw new InvalidRequestException(String.format("Could not find the role in the scope %s", scopeIdentifier));
    }
    return deleteInternal(identifier, scopeIdentifier);
  }

  @Override
  public void deleteIfPresent(String identifier, String scopeIdentifier) {
    Optional<ResourceGroup> currentResourceGroupOptional = get(identifier, scopeIdentifier);
    if (currentResourceGroupOptional.isPresent()) {
      deleteInternal(identifier, scopeIdentifier);
    }
  }

  private ResourceGroup deleteInternal(String identifier, String scopeIdentifier) {
    return Failsafe.with(deleteResourceGroupTransactionPolicy).get(() -> transactionTemplate.execute(status -> {
      long deleteCount = roleAssignmentService.deleteMany(
          scopeIdentifier, RoleAssignmentFilter.builder().resourceGroupFilter(Sets.newHashSet(identifier)).build());
      return resourceGroupDao.delete(identifier, scopeIdentifier)
          .orElseThrow(()
                           -> new UnexpectedException(String.format(
                               "Failed to delete the resource group %s in the scope %s", identifier, scopeIdentifier)));
    }));
  }
}

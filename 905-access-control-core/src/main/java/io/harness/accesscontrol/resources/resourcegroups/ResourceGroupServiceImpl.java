package io.harness.accesscontrol.resources.resourcegroups;

import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDao;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@ValidateOnExecution
public class ResourceGroupServiceImpl implements ResourceGroupService {
  private final ResourceGroupDao resourceGroupDao;

  @Inject
  public ResourceGroupServiceImpl(ResourceGroupDao resourceGroupDao) {
    this.resourceGroupDao = resourceGroupDao;
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
  public Optional<ResourceGroup> get(String identifier, String scopeIdentifier) {
    return resourceGroupDao.get(identifier, scopeIdentifier);
  }

  @Override
  public ResourceGroup delete(String identifier, String scopeIdentifier) {
    Optional<ResourceGroup> currentResourceGroupOptional = get(identifier, scopeIdentifier);
    if (!currentResourceGroupOptional.isPresent()) {
      throw new InvalidRequestException(String.format("Could not find the role in the scope %s", scopeIdentifier));
    }
    return resourceGroupDao.delete(identifier, scopeIdentifier)
        .orElseThrow(()
                         -> new UnexpectedException(String.format(
                             "Failed to delete the role %s in the scope %s", identifier, scopeIdentifier)));
  }
}

package io.harness.accesscontrol.resources.resourcegroups.persistence;

import static io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBOMapper.fromDBO;
import static io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBOMapper.toDBO;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Singleton
@ValidateOnExecution
public class ResourceGroupDaoImpl implements ResourceGroupDao {
  private final ResourceGroupRepository resourceGroupRepository;

  @Inject
  public ResourceGroupDaoImpl(ResourceGroupRepository resourceGroupRepository) {
    this.resourceGroupRepository = resourceGroupRepository;
  }

  @Override
  public ResourceGroup upsert(ResourceGroup resourceGroupUpdate) {
    ResourceGroupDBO resourceGroupUpdateDBO = toDBO(resourceGroupUpdate);
    Optional<ResourceGroupDBO> resourceGroupDBO = resourceGroupRepository.findByIdentifierAndScopeIdentifier(
        resourceGroupUpdate.getIdentifier(), resourceGroupUpdate.getScopeIdentifier());
    if (resourceGroupDBO.isPresent()) {
      resourceGroupUpdateDBO.setId(resourceGroupDBO.get().getId());
      resourceGroupUpdateDBO.setVersion(resourceGroupDBO.get().getVersion());
      resourceGroupUpdateDBO.setCreatedAt(resourceGroupDBO.get().getCreatedAt());
      resourceGroupUpdateDBO.setLastModifiedAt(resourceGroupDBO.get().getCreatedAt());
    }
    return fromDBO(resourceGroupRepository.save(resourceGroupUpdateDBO));
  }

  @Override
  public PageResponse<ResourceGroup> list(PageRequest pageRequest, String scopeIdentifier) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Page<ResourceGroupDBO> resourceGroupPages =
        resourceGroupRepository.findByScopeIdentifier(scopeIdentifier, pageable);
    return PageUtils.getNGPageResponse(resourceGroupPages.map(ResourceGroupDBOMapper::fromDBO));
  }

  @Override
  public Optional<ResourceGroup> get(String identifier, String scopeIdentifier) {
    return resourceGroupRepository.findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .flatMap(r -> Optional.of(fromDBO(r)));
  }

  @Override
  public Optional<ResourceGroup> delete(String identifier, String scopeIdentifier) {
    return resourceGroupRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .flatMap(r -> Optional.of(fromDBO(r)));
  }
}

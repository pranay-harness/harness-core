/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.resources.resourcegroups.persistence;

import static io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBOMapper.fromDBO;
import static io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBOMapper.toDBO;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroup;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO.ResourceGroupDBOKeys;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PL)
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
    Optional<ResourceGroupDBO> resourceGroupOpt = resourceGroupRepository.findByIdentifierAndScopeIdentifier(
        resourceGroupUpdate.getIdentifier(), resourceGroupUpdate.getScopeIdentifier());
    if (resourceGroupOpt.isPresent()) {
      ResourceGroupDBO currentResourceGroupDBO = resourceGroupOpt.get();
      if (currentResourceGroupDBO.equals(resourceGroupUpdateDBO)) {
        return fromDBO(currentResourceGroupDBO);
      }
      resourceGroupUpdateDBO.setId(currentResourceGroupDBO.getId());
      resourceGroupUpdateDBO.setVersion(currentResourceGroupDBO.getVersion());
      resourceGroupUpdateDBO.setCreatedAt(currentResourceGroupDBO.getCreatedAt());
      resourceGroupUpdateDBO.setLastModifiedAt(currentResourceGroupDBO.getCreatedAt());
      resourceGroupUpdateDBO.setNextReconciliationIterationAt(
          currentResourceGroupDBO.getNextReconciliationIterationAt());
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
  public List<ResourceGroup> list(List<String> resourceGroupIdentifiers, String scopeIdentifier) {
    Criteria criteria = Criteria.where(ResourceGroupDBOKeys.scopeIdentifier)
                            .is(scopeIdentifier)
                            .and(ResourceGroupDBOKeys.identifier)
                            .in(resourceGroupIdentifiers);
    List<ResourceGroupDBO> resourceGroupDBOs = resourceGroupRepository.findAllWithCriteria(criteria);
    return resourceGroupDBOs.stream().map(ResourceGroupDBOMapper::fromDBO).collect(Collectors.toList());
  }

  @Override
  public Optional<ResourceGroup> get(String identifier, String scopeIdentifier) {
    return resourceGroupRepository.findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .flatMap(r -> Optional.of(fromDBO(r)));
  }

  @Override
  public Optional<ResourceGroup> delete(String identifier, String scopeIdentifier) {
    return resourceGroupRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .stream()
        .findFirst()
        .flatMap(r -> Optional.of(fromDBO(r)));
  }
}

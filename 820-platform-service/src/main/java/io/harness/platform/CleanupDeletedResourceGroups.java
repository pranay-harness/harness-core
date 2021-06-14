package io.harness.platform;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.framework.repositories.spring.ResourceGroupRepository;
import io.harness.resourcegroup.model.ResourceGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class CleanupDeletedResourceGroups {
  private final ResourceGroupRepository resourceGroupRepository;

  public void cleanUp() {
    Page<ResourceGroup> deletedResourceGroups =
        resourceGroupRepository.findAll(Criteria.where("deleted").is(Boolean.TRUE), Pageable.unpaged());
    resourceGroupRepository.deleteAll(deletedResourceGroups);
  }
}

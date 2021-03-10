package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.annotation.HarnessRepo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ResourceGroupRepository extends PagingAndSortingRepository<ResourceGroupDBO, String> {
  Optional<ResourceGroupDBO> findByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);
  Page<ResourceGroupDBO> findByScopeIdentifier(String scopeIdentifier, Pageable pageable);
  Optional<ResourceGroupDBO> deleteByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);
}

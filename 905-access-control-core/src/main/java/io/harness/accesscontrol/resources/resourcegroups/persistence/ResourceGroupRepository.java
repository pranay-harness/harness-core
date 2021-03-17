package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.annotation.HarnessRepo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ResourceGroupRepository
    extends PagingAndSortingRepository<ResourceGroupDBO, String>, ResourceGroupCustomRepository {
  Optional<ResourceGroupDBO> findByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);

  Page<ResourceGroupDBO> findByScopeIdentifier(String scopeIdentifier, Pageable pageable);

  List<ResourceGroupDBO> deleteByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);
}

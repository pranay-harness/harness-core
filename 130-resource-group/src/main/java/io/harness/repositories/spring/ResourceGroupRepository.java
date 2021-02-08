package io.harness.repositories.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.ResourceGroup;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface ResourceGroupRepository extends PagingAndSortingRepository<ResourceGroup, String> {
  Optional<ResourceGroup> findDistinctById(String resourceGroupId);

  Optional<ResourceGroup> findDistinctByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<ResourceGroup> deleteByIdentifierAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String identifier, String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

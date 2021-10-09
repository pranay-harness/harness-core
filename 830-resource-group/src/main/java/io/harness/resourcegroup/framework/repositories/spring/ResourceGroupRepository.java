package io.harness.resourcegroup.framework.repositories.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.framework.repositories.custom.ResourceGroupRepositoryCustom;
import io.harness.resourcegroup.model.ResourceGroup;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface ResourceGroupRepository
    extends PagingAndSortingRepository<ResourceGroup, String>, ResourceGroupRepositoryCustom {
  List<ResourceGroup> deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

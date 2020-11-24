package io.harness.repositories.core.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entities.Organization;
import io.harness.repositories.core.custom.OrganizationRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface OrganizationRepository
    extends PagingAndSortingRepository<Organization, String>, OrganizationRepositoryCustom {
  Optional<Organization> findByAccountIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String identifier, boolean notDeleted);
}

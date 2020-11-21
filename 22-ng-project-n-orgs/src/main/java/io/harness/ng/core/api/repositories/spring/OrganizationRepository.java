package io.harness.ng.core.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.api.repositories.custom.OrganizationRepositoryCustom;
import io.harness.ng.core.entities.Organization;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface OrganizationRepository
    extends PagingAndSortingRepository<Organization, String>, OrganizationRepositoryCustom {
  Optional<Organization> findByAccountIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String identifier, boolean notDeleted);
}

package io.harness.ng.core.dao.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.dao.api.repositories.custom.OrganizationRepositoryCustom;
import io.harness.ng.core.entities.Organization;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface OrganizationRepository
    extends PagingAndSortingRepository<Organization, String>, OrganizationRepositoryCustom {
  Optional<Organization> findByAccountIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String organizationIdentifier, boolean notDeleted);
}

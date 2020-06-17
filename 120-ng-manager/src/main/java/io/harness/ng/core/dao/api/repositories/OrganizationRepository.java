package io.harness.ng.core.dao.api.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entities.Organization;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface OrganizationRepository extends PagingAndSortingRepository<Organization, String> {
  Optional<Organization> findByIdAndDeletedNot(String id, boolean deleted);
}

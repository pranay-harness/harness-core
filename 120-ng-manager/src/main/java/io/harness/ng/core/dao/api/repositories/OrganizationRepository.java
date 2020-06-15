package io.harness.ng.core.dao.api.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entities.Organization;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

@HarnessRepo
public interface OrganizationRepository extends PagingAndSortingRepository<Organization, String> {
  List<Organization> findByAccountId(String accountId);
}

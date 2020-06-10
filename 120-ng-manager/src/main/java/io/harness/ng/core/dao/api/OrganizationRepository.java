package io.harness.ng.core.dao.api;

import io.harness.ng.core.entities.Organization;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface OrganizationRepository extends PagingAndSortingRepository<Organization, String> {
  List<Organization> findByAccountId(String accountId);
}

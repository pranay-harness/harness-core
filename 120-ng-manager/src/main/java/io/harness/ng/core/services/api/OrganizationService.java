package io.harness.ng.core.services.api;

import io.harness.ng.core.entities.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface OrganizationService {
  Organization create(Organization organization);

  Optional<Organization> get(String organizationId);

  Organization update(Organization organization);

  Page<Organization> list(String accountId, Criteria criteria, Pageable pageable);

  boolean delete(String organizationId);
}

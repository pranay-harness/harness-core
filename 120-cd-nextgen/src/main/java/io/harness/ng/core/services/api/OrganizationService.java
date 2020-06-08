package io.harness.ng.core.services.api;

import io.harness.ng.core.entities.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationService {
  Organization create(Organization organization);

  Optional<Organization> get(String organizationId);

  Organization update(Organization organization);

  List<Organization> getAll(String accountId);
}

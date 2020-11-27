package io.harness.ng.core.services;

import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface OrganizationService {
  Organization create(String accountIdentifier, OrganizationDTO organization);

  Optional<Organization> get(String accountIdentifier, String identifier);

  Organization update(String accountIdentifier, String identifier, OrganizationDTO organization);

  Page<Organization> list(String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO);

  Page<Organization> list(Criteria criteria, Pageable pageable);

  boolean delete(String accountIdentifier, String identifier, Long version);
}

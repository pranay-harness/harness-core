package io.harness.ng.core.api;

import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AggregateOrganizationService {
  OrganizationAggregateDTO getOrganizationAggregateDTO(String accountIdentifier, String identifier);

  Page<OrganizationAggregateDTO> listOrganizationAggregateDTO(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO);
}

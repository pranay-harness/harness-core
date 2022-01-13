/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
public interface AggregateOrganizationService {
  OrganizationAggregateDTO getOrganizationAggregateDTO(String accountIdentifier, String identifier);

  Page<OrganizationAggregateDTO> listOrganizationAggregateDTO(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO);
}

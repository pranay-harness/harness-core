package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.AccountResourcesDTO;

@OwnedBy(PL)
public interface AggregateAccountResourceService {
  AccountResourcesDTO getAccountResourcesDTO(String accountIdentifier);
}

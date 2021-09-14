/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.serviceaccounts.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.RoleAssignmentMetadataDTO;
import io.harness.serviceaccount.ServiceAccountDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class ServiceAccountAggregateDTO {
  @NotNull ServiceAccountDTO serviceAccount;
  @NotNull Long createdAt;
  @NotNull Long lastModifiedAt;

  int tokensCount;
  List<RoleAssignmentMetadataDTO> roleAssignmentsMetadataDTO;
}

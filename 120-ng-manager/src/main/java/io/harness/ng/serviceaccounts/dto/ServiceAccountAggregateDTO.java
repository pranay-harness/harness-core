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

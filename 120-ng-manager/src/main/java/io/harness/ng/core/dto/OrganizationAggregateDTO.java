package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.dto.UserSearchDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class OrganizationAggregateDTO {
  @NotNull OrganizationResponse organizationResponse;
  int projectsCount;

  List<UserSearchDTO> admins;
  List<UserSearchDTO> collaborators;
}

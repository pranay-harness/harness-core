package io.harness.ng.core.dto;

import io.harness.ng.core.invites.dto.UserSearchDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectAggregateDTO {
  @NotNull ProjectResponse projectResponse;
  OrganizationDTO organization;
  boolean harnessManagedOrg;

  List<UserSearchDTO> admins;
  List<UserSearchDTO> collaborators;
}

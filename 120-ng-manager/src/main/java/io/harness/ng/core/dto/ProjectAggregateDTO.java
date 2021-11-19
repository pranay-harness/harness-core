package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Schema(name = "ProjectAggregate", description = "This is the view of the ProjectAggregate entity defined in Harness")
public class ProjectAggregateDTO {
  @NotNull ProjectResponse projectResponse;
  OrganizationDTO organization;
  boolean harnessManagedOrg;

  List<UserMetadataDTO> admins;
  List<UserMetadataDTO> collaborators;
}

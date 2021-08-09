package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class UserGroupAggregateDTO {
  @NotNull UserGroupDTO userGroupDTO;
  List<UserMetadataDTO> users;
  List<RoleAssignmentMetadataDTO> roleAssignmentsMetadataDTO;
  long lastModifiedAt;
}

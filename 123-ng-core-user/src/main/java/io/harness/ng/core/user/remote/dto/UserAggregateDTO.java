package io.harness.ng.core.user.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.RoleAssignmentMetadataDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(value = "UserAggregate")
@OwnedBy(PL)
public class UserAggregateDTO {
  @ApiModelProperty(required = true) UserMetadataDTO user;
  @ApiModelProperty(required = true) List<RoleAssignmentMetadataDTO> roleAssignmentMetadata;
}

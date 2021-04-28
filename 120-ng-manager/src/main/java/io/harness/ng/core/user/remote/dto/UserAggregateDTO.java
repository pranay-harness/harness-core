package io.harness.ng.core.user.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.remote.RoleBinding;

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
  @ApiModelProperty(required = true) UserSearchDTO user;
  List<RoleBinding> roleBindings;
}

package io.harness.ng.core.acl.user;

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
@ApiModel(value = "ACLUserAggregate")
@OwnedBy(PL)
public class ACLUserAggregateDTO {
  @ApiModelProperty(required = true) UserSearchDTO user;
  List<RoleBinding> roleBindings;
  @ApiModelProperty(required = true) Status status;
  public enum Status { ACTIVE, INVITED, REQUESTED }
}

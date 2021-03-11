package io.harness.ng.core.acl;

import io.harness.ng.core.invites.dto.UserSearchDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(value = "ACLUserAggregate")
public class ACLUserAggregateDTO {
  @ApiModelProperty(required = true) UserSearchDTO user;
  List<RoleAssignment> roleAssignments;
  @ApiModelProperty(required = true) Status status;

  @Data
  @Builder
  public static class RoleAssignment {
    @ApiModelProperty(required = true) String roleIdentifier;
    @ApiModelProperty(required = true) String roleName;
    @ApiModelProperty(required = true) String resourceGroupIdentifier;
    @ApiModelProperty(required = true) String resourceGroupName;
    @ApiModelProperty(required = true) boolean managedRole;
  }

  public enum Status { ACTIVE, INVITED, REQUESTED }
}

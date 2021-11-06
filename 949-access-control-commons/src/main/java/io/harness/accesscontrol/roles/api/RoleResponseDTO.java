package io.harness.accesscontrol.roles.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@ApiModel(value = "RoleResponse")
@Schema(name = "RoleResponse")
public class RoleResponseDTO {
  @ApiModelProperty(required = true) RoleDTO role;
  @ApiModelProperty(required = true) ScopeDTO scope;
  boolean harnessManaged;
  Long createdAt;
  Long lastModifiedAt;
}

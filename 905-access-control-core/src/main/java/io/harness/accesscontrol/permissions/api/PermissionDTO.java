package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ApiModel(value = "Permission")
public class PermissionDTO {
  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  String resourceType;
  String action;
  @ApiModelProperty(required = true) PermissionStatus status;
  @ApiModelProperty(required = true, allowableValues = "account, org, project") @NotEmpty Set<String> scopes;
}

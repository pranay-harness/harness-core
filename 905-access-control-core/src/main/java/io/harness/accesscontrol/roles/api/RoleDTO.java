package io.harness.accesscontrol.roles.api;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ApiModel(value = "Role")
public class RoleDTO {
  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  @ApiModelProperty(required = true, allowableValues = "account, org, project") @NotEmpty Set<String> scopes;
  @NotEmpty Set<String> permissions;
  String description;
  Map<String, String> tags;
}

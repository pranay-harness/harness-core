package io.harness.accesscontrol.roles.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ApiModel(value = "RoleResponse")
public class RoleResponseDTO {
  @ApiModelProperty(required = true) @NotNull RoleDTO role;
  @NotEmpty String parentIdentifier;
  boolean harnessManaged;
  Long createdAt;
  Long lastModifiedAt;
}

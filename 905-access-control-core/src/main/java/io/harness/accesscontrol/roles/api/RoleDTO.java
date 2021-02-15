package io.harness.accesscontrol.roles.api;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldNameConstants(innerTypeName = "RoleDTOKeys")
@ApiModel(value = RoleDTO.MODEL_NAME)
public class RoleDTO {
  public static final String MODEL_NAME = "Role";

  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  @ApiModelProperty(required = true) @NotEmpty Set<String> permissions;
  @NotEmpty Set<String> allowedScopeLevels;
  String description;
  Map<String, String> tags;
}

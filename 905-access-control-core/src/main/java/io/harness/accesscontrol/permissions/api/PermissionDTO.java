package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PermissionDTOKeys")
@ApiModel(value = PermissionDTO.MODEL_NAME)
public class PermissionDTO {
  public static final String MODEL_NAME = "Permission";

  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  @ApiModelProperty(required = true) PermissionStatus status;
  @ApiModelProperty(required = true) @NotEmpty Set<String> scopes;
  String resourceType;
  String action;
}

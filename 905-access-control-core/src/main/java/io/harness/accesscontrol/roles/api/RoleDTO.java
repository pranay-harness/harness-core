package io.harness.accesscontrol.roles.api;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "RoleDTOKeys")
@ApiModel(value = RoleDTO.MODEL_NAME)
public class RoleDTO {
  public static final String MODEL_NAME = "Role";

  @ApiModelProperty(required = true) @EntityIdentifier final String identifier;
  @ApiModelProperty(required = true) @NGEntityName final String name;
  final Set<String> permissions;
  @Setter Set<String> allowedScopeLevels;
  final String description;
  final Map<String, String> tags;
}

package io.harness.accesscontrol.roles.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

@OwnedBy(HarnessTeam.PL)
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "RoleDTOKeys")
@ApiModel(value = RoleDTO.MODEL_NAME)
@Schema(name = RoleDTO.MODEL_NAME)
public class RoleDTO {
  public static final String MODEL_NAME = "Role";

  @ApiModelProperty(required = true) final String identifier;
  @ApiModelProperty(required = true) final String name;
  final Set<String> permissions;
  @Setter Set<String> allowedScopeLevels;
  final String description;
  final Map<String, String> tags;
}

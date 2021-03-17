package io.harness.accesscontrol.roleassignments.api;

import io.harness.accesscontrol.principals.PrincipalDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@ApiModel(value = "RoleAssignment")
public class RoleAssignmentDTO {
  final String identifier;
  @ApiModelProperty(required = true) final String resourceGroupIdentifier;
  @ApiModelProperty(required = true) final String roleIdentifier;
  @ApiModelProperty(required = true) final PrincipalDTO principal;
  @Setter boolean disabled;
}
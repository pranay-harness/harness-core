package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "RoleBindingKeys")
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class RoleBinding {
  @ApiModelProperty(required = true) String roleIdentifier;
  @ApiModelProperty(required = true) String roleName;
  String resourceGroupIdentifier;
  String resourceGroupName;
  @ApiModelProperty(required = true) @Builder.Default Boolean managedRole = Boolean.FALSE;
}
/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.InviteType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Invite")
@OwnedBy(PL)
@Schema(name = "Invite", description = "This is the view of the Invite entity defined in Harness")
public class InviteDTO {
  @ApiModelProperty(required = true) String id;
  @ApiModelProperty(required = true) String name;
  @ApiModelProperty(required = true) @NotEmpty @Email String email;
  @ApiModelProperty(required = true) String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  List<RoleBinding> roleBindings;
  List<String> userGroups;
  @ApiModelProperty(required = true) @NotNull InviteType inviteType;
  @Builder.Default Boolean approved = false;
}

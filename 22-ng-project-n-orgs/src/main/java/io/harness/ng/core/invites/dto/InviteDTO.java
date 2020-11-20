package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite.InviteType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class InviteDTO {
  String id;
  @ApiModelProperty(required = true) String name;
  @ApiModelProperty(required = true) @NotEmpty @Email String email;
  @ApiModelProperty(required = true) RoleDTO role;
  @ApiModelProperty(required = true) @NotNull InviteType inviteType;
  @ApiModelProperty(required = true, dataType = "boolean") @Builder.Default Boolean approved = false;
}

package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite.InviteType;
import io.harness.ng.core.invites.remote.RoleBinding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
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
@OwnedBy(PL)
public class InviteDTO {
  String id;
  @ApiModelProperty(required = true) String name;
  @ApiModelProperty(required = true) @NotEmpty @Email String email;
  @ApiModelProperty(required = true) @NotEmpty List<RoleBinding> roleBindings;
  @ApiModelProperty(required = true) @NotNull InviteType inviteType;
  @ApiModelProperty(required = true, dataType = "boolean") @Builder.Default Boolean approved = false;
}

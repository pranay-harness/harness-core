package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.invites.entities.Invite.InviteType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class CreateInviteListDTO {
  @ApiModelProperty(required = true) @NotEmpty @Size(max = 100) List<String> users;
  @ApiModelProperty(required = true) @Valid RoleDTO role;
  @ApiModelProperty(required = true) InviteType inviteType;
}

package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.invites.entities.Invite.InviteType;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
public class CreateInviteListDTO {
  @ApiModelProperty(required = true) @NotEmpty @Size(max = 100) List<String> users;
  @ApiModelProperty(required = true) @NotEmpty List<RoleAssignmentDTO> roleAssignments;
  @ApiModelProperty(required = true) InviteType inviteType;
}

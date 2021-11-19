package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.InviteType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@ApiModel(value = "CreateInvite")
@OwnedBy(PL)
@Schema(name = "CreateInvite", description = "This is the view of the CreateInvite entity defined in Harness")
public class CreateInviteDTO {
  @ApiModelProperty(required = true) @NotEmpty @Size(max = 100) List<String> users;
  @ApiModelProperty(required = true) @NotEmpty List<RoleBinding> roleBindings;
  @ApiModelProperty(required = true) InviteType inviteType;
  List<String> userGroups;
}

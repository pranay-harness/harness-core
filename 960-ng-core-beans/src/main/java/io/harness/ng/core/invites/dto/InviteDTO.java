package io.harness.ng.core.invites.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.InviteType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
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
@ApiModel(value = "Invite")
@OwnedBy(PL)
public class InviteDTO {
  @ApiModelProperty(required = true) String id;
  @ApiModelProperty(required = true) String name;
  @ApiModelProperty(required = true) @NotEmpty @Email String email;
  @ApiModelProperty(required = true) String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @ApiModelProperty(required = true) @NotEmpty List<RoleBinding> roleBindings;
  List<String> userGroups;
  @ApiModelProperty(required = true) @NotNull InviteType inviteType;
  @Builder.Default Boolean approved = false;
}

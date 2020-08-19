package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.ng.core.models.InviteType;
import io.swagger.annotations.ApiModelProperty;
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
public class InviteDTO {
  @ApiModelProperty(required = true) String name;
  @ApiModelProperty(required = true) @NotEmpty @Email String email;
  @ApiModelProperty(required = true) RoleDTO role;
  @ApiModelProperty(required = true) InviteType inviteType;
  @ApiModelProperty(required = true, dataType = "boolean") @Builder.Default Boolean approved = false;
}
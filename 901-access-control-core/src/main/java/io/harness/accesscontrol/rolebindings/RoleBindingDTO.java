package io.harness.accesscontrol.rolebindings;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.data.validator.EntityIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "RoleBinding")
public class RoleBindingDTO {
  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) String parentIdentifier;
  @NotEmpty String resourceGroupIdentifier;
  @NotEmpty String roleIdentifier;
  @NotEmpty String principalIdentifier;
  @NotNull PrincipalType principalType;
  boolean isDefault;
  boolean isDisabled;
  long version;
}

package io.harness.accesscontrol.roles;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Role")
public class RoleDTO {
  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) String parentIdentifier;
  @ApiModelProperty(required = true) @NGEntityName String displayName;
  @NotEmpty Set<String> validScopes;
  @NotEmpty Set<String> permissions;
  boolean isDefault;
  long version;
}

package io.harness.accesscontrol.permissions;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
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
@ApiModel(value = "Permission")
public class PermissionDTO {
  @ApiModelProperty(required = true) @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NGEntityName String displayName;
  @NotEmpty String resourceType;
  @NotEmpty String action;
  @NotNull PermissionStatus status;
  @NotEmpty Set<String> validScopes;
  long version;
}

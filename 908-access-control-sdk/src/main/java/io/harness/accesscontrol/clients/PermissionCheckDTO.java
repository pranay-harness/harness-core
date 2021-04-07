package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder(builderClassName = "Builder")
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "PermissionCheck")
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
public class PermissionCheckDTO {
  ResourceScope resourceScope;
  @NotEmpty String resourceType;
  @NotEmpty String resourceIdentifier;
  @NotEmpty String permission;
}

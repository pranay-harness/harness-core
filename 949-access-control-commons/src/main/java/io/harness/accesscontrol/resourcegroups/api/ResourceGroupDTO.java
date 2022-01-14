package io.harness.accesscontrol.resourcegroups.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@ApiModel(value = "ResourceGroup")
@Schema(name = "ResourceGroup")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResourceGroupDTO {
  @NotEmpty String identifier;
  @NotEmpty String name;
}

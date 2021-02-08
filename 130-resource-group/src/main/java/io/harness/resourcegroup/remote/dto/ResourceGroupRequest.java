package io.harness.resourcegroup.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResourceGroupRequest {
  @Valid @NotNull @JsonProperty("resourcegroup") private ResourceGroupDTO resourceGroupDTO;
}

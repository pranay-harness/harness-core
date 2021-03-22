package io.harness.resourcegroup.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class ResourceGroupRequest {
  @Valid @NotNull @JsonProperty("resourcegroup") private ResourceGroupDTO resourceGroup;
}

package io.harness.resourcegroup.remote.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class ResourceGroupRequest implements YamlDTO {
  @Valid @NotNull @JsonProperty("resourcegroup") private ResourceGroupDTO resourceGroup;
}

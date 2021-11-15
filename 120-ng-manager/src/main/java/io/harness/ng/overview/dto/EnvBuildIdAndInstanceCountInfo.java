package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class EnvBuildIdAndInstanceCountInfo {
  private String envId;
  private String envName;
  private List<BuildIdAndInstanceCount> buildIdAndInstanceCountList;
}

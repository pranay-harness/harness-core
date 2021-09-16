package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
public class BuildsByEnvironment {
  private String envId;
  private List<InstanceDTOsByBuildId> builds;
}

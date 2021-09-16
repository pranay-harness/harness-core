package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InstanceDTO;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
public class InstanceDTOsByBuildId {
  private String buildId;
  private List<InstanceDTO> instances;
}

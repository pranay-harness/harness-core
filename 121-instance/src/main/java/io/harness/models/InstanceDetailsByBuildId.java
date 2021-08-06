package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
public class InstanceDetailsByBuildId {
  String buildId;
  List<InstanceDetailsDTO> instances;
}

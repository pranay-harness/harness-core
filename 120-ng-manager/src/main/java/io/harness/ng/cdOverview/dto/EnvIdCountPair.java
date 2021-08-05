package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
@AllArgsConstructor
public class EnvIdCountPair {
  String envId;
  int count;
}

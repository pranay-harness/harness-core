package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
public class EnvBuildInstanceCount {
  private String envIdentifier;
  private String envName;
  private String tag;
  private int count;
}

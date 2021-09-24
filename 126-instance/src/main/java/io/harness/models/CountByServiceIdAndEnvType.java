package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.EnvironmentType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
public class CountByServiceIdAndEnvType {
  private String serviceIdentifier;
  private EnvironmentType envType;
  private int count;
}

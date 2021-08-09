package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;

@lombok.Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class Nexus3RequestData {
  private String repositoryName;
  private String node;
}

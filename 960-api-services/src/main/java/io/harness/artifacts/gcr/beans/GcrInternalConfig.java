package io.harness.artifacts.gcr.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class GcrInternalConfig {
  String registryHostname;
  String basicAuthHeader;
}

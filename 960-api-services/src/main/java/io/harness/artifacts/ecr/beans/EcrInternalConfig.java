package io.harness.artifacts.ecr.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class EcrInternalConfig {
  String region;
  String accessKey;
  char[] secretKey;
}

package io.harness.artifacts.ecr.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcrInternalConfig {
  String region;
  String accessKey;
  char [] secretKey;
}

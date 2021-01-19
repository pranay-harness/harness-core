package io.harness.artifacts.gcr.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GcrInternalConfig {
  String registryHostname;
  String gcrUrl;
  String basicAuthHeader;
}

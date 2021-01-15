package io.harness.artifacts.gcr.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GcpInternalConfig {
  String gcrHostName;
  String basicAuthHeader;
}
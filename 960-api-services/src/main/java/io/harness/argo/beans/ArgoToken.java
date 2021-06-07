package io.harness.argo.beans;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ArgoToken {
  private String token;
}

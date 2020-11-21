package io.harness.cdng.k8s;

import io.harness.data.Outcome;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRollingOutcome implements Outcome {
  private String releaseName;
  private int releaseNumber;
}

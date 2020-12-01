package io.harness.cdng.k8s;

import io.harness.pms.sdk.core.data.Outcome;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("k8sRollingOutcome")
public class K8sRollingOutcome implements Outcome {
  private String releaseName;
  private int releaseNumber;
}

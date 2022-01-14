package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("k8sRollingOutcome")
@JsonTypeName("k8sRollingOutcome")
@RecasterAlias("io.harness.cdng.k8s.K8sRollingOutcome")
public class K8sRollingOutcome implements Outcome, ExecutionSweepingOutput {
  String releaseName;
  int releaseNumber;
}

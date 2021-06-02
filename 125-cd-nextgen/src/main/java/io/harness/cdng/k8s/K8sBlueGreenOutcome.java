package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

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
@TypeAlias("k8sBlueGreenOutcome")
@JsonTypeName("k8sBlueGreenOutcome")
public class K8sBlueGreenOutcome implements Outcome, ExecutionSweepingOutput {
  private int releaseNumber;
  private String releaseName;
  private String primaryServiceName;
  private String stageServiceName;
  private String stageColor;
  private String primaryColor;
}

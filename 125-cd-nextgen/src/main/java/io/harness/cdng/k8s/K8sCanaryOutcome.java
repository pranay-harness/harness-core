package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(CDP)
@TypeAlias("k8sCanaryOutcome")
@JsonTypeName("k8sCanaryOutcome")
public class K8sCanaryOutcome implements Outcome, ExecutionSweepingOutput {
  String releaseName;
  Integer targetInstances;
  Integer releaseNumber;
  String canaryWorkload;
  boolean canaryWorkloadDeployed;

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sCanaryOutcome.builder()
                                                       .releaseName(releaseName)
                                                       .targetInstances(targetInstances)
                                                       .releaseNumber(releaseNumber)
                                                       .canaryWorkload(canaryWorkload)
                                                       .build());
  }
}

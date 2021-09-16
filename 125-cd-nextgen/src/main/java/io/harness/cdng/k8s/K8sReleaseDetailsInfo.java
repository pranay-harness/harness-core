package io.harness.cdng.k8s;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.executables.StepDetailsInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.k8s.K8sReleaseDetailsInfo")
public class K8sReleaseDetailsInfo implements StepDetailsInfo {
  String releaseName;
}

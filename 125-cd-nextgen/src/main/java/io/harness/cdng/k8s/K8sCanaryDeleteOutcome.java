package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(CDP)
@TypeAlias("k8sCanaryDeleteOutcome")
@JsonTypeName("k8sCanaryDeleteOutcome")
@RecasterAlias("io.harness.cdng.k8s.K8sCanaryDeleteOutcome")
public class K8sCanaryDeleteOutcome implements ExecutionSweepingOutput {}

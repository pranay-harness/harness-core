package io.harness.cdng.service.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("serviceDefinitionStepParameters")
@RecasterAlias("io.harness.cdng.service.steps.ServiceDefinitionStepParameters")
public class ServiceDefinitionStepParameters implements StepParameters {
  String type;
  String childNodeId;
}

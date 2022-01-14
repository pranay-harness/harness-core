package io.harness.pms.sdk.core.supporter.child;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class TestSectionStepParameters implements StepParameters {
  String childNodeId;
  Map<String, String> data;
}

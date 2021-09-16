package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@JsonTypeName("Dummy1")
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.utils.DummyVisualizationOutcome")
public class DummyVisualizationOutcome implements Outcome {
  String test;
}

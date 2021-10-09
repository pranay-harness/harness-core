package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("dummySweepingOutput")
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.utils.DummySweepingOutput")
public class DummySweepingOutput implements ExecutionSweepingOutput {
  String test;
}

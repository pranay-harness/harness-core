package io.harness.pms.sdk.core.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import com.google.protobuf.ByteString;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class ChainDetails {
  boolean shouldEnd;
  PassThroughData passThroughData;
  ByteString passThroughBytes;
}

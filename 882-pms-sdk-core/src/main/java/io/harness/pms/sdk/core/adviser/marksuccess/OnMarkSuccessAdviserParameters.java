package io.harness.pms.sdk.core.adviser.marksuccess;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnMarkSuccessAdviserParameters {
  String nextNodeId;
}

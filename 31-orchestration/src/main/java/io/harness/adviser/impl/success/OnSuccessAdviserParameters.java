package io.harness.adviser.impl.success;

import io.harness.adviser.AdviserParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnSuccessAdviserParameters implements AdviserParameters {
  String nextNodeId;
}

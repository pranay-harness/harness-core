package io.harness.adviser.impl.success;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class OnSuccessAdviserParameters implements AdviserParameters {
  String nextNodeId;
}

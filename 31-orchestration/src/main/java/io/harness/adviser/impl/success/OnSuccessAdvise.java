package io.harness.adviser.impl.success;

import io.harness.adviser.Advise;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
@Redesign
public class OnSuccessAdvise implements Advise {
  @Default String type = "ON_SUCCESS";
  String nextNodeId;
}

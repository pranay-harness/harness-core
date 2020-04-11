package io.harness.adviser.impl.success;

import io.harness.adviser.Advise;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnSuccessAdvise implements Advise {
  String nextNodeId;
}

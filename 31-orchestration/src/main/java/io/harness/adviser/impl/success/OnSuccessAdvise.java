package io.harness.adviser.impl.success;

import io.harness.adviser.Advise;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Redesign
public class OnSuccessAdvise implements Advise {
  String type = "ON_SUCCESS";
  String nextNodeId;

  @Builder
  public OnSuccessAdvise(String nextNodeId) {
    this.nextNodeId = nextNodeId;
  }
}

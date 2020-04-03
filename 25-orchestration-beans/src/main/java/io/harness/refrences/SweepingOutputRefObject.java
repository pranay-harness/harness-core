package io.harness.refrences;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class SweepingOutputRefObject implements RefObject {
  String name;
  String producerId;
  RefType refType = RefType.SWEEPING_OUTPUT;
}

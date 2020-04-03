package io.harness.refrences;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class ContextRefObject implements RefObject {
  String name;
  String producerId;
  RefType refType = RefType.CONTEXT;
}

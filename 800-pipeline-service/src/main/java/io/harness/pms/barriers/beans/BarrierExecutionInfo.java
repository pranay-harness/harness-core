package io.harness.pms.barriers.beans;

import io.harness.steps.barriers.beans.StageDetail;

import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BarrierExecutionInfo {
  String name;
  String identifier;
  long startedAt;
  boolean started;
  long timeoutIn;
  Set<StageDetail> stages;
}

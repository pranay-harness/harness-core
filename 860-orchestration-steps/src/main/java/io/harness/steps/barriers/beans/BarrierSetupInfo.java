package io.harness.steps.barriers.beans;

import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierSetupInfoKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BarrierSetupInfo {
  String name;
  String identifier;
  long timeout;
  Set<StageDetail> stages;
}
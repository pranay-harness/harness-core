package io.harness.pms.barriers.beans;

import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BarrierSetupInfo {
  String name;
  String identifier;
  Set<StageDetail> stages;
}
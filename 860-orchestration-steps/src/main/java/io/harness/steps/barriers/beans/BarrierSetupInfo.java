package io.harness.steps.barriers.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierSetupInfoKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BarrierSetupInfo {
  String name;
  String identifier;
  Set<StageDetail> stages;
}
package io.harness.steps.barriers.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "StageDetailKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PIPELINE)
public class StageDetail {
  String name;
  String identifier;
}

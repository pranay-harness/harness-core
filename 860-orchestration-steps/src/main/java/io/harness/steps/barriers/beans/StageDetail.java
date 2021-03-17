package io.harness.steps.barriers.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "StageDetailKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StageDetail {
  String name;
  String identifier;
}

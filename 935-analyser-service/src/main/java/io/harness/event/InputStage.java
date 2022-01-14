package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "InputStageKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InputStage {
  String stage;
  Map<String, String> keyPattern;
  boolean isMultiKey;
  boolean isEof;
  long keysExamined;
  InputStage inputStage;
}
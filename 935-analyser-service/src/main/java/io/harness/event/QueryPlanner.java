/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.query.SortPattern;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "QueryExplainResultKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QueryPlanner {
  WinningPlan winningPlan;
  String namespace;
  ParsedQuery parsedQuery;

  @OwnedBy(PIPELINE)
  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "WinningPlanKeys")
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class WinningPlan {
    String stage;
    InputStage inputStage;
    SortPattern sortPattern;
  }
}

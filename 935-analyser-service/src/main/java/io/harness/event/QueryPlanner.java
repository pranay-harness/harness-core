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

package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "QueryExplainResultKeys")
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("queryExplainResult")
public class QueryExplainResult {
  QueryPlanner queryPlanner;
  ExecutionStats executionStats;
}

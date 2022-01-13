/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;

import software.wings.sm.StateExecutionData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.RandomStringUtils;

@OwnedBy(CDC)
@Value
@Builder
public class ExecutionHistoryMetadata implements ExecutionDetailsMetadata {
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String name;
  ExecutionStatus status;
  Map<String, Object> executionDetails;

  // activityId is used to fill up subCommands later when we want to query execution logs.
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String activityId;
  @NonFinal @Setter List<ActivityCommandUnitMetadata> subCommands;

  TimingMetadata timing;

  public static List<ExecutionHistoryMetadata> fromStateExecutionDataList(
      List<StateExecutionData> stateExecutionDataList) {
    if (isEmpty(stateExecutionDataList)) {
      return null;
    }

    return IntStream.range(0, stateExecutionDataList.size())
        .mapToObj(i -> fromStateExecutionData(i, stateExecutionDataList.get(i)))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  static ExecutionHistoryMetadata fromStateExecutionData(int index, StateExecutionData stateExecutionData) {
    if (stateExecutionData == null) {
      return null;
    }

    String name = format("%s_%d",
        stateExecutionData.getStateName() == null ? generateRandomName() : stateExecutionData.getStateName(),
        index + 1);
    ExecutionHistoryMetadataBuilder executionHistoryMetadataBuilder =
        ExecutionHistoryMetadata.builder().name(name).status(stateExecutionData.getStatus());
    updateWithExecutionDetails(stateExecutionData, executionHistoryMetadataBuilder);
    return executionHistoryMetadataBuilder.build();
  }

  private static void updateWithExecutionDetails(@NotNull StateExecutionData stateExecutionData,
      @NotNull ExecutionHistoryMetadataBuilder executionHistoryMetadataBuilder) {
    ExecutionDetailsInternalMetadata executionDetailsInternalMetadata =
        ExecutionDetailsInternalMetadata.fromStateExecutionData(stateExecutionData);
    if (executionDetailsInternalMetadata == null) {
      return;
    }

    executionHistoryMetadataBuilder.executionDetails(executionDetailsInternalMetadata.getExecutionDetails())
        .activityId(executionDetailsInternalMetadata.getActivityId())
        .timing(executionDetailsInternalMetadata.getTiming());
  }

  private static String generateRandomName() {
    return RandomStringUtils.randomAlphanumeric(6);
  }
}

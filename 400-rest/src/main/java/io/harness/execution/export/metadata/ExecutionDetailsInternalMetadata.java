/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmptyMap;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.GraphNode;
import software.wings.sm.StateExecutionData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
class ExecutionDetailsInternalMetadata {
  private static final String ACTIVITY_ID_KEY = "activityId";
  private static final Set<String> IGNORED_KEYS =
      ImmutableSet.<String>builder().add("duration", "Unit", "Harness Owned").build();

  InstanceCountMetadata instanceCount;
  Map<String, Object> executionDetails;

  // activityId is used to fill up subCommands later when we want to query execution logs.
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String activityId;

  TimingMetadata timing;

  static ExecutionDetailsInternalMetadata fromGraphNode(GraphNode node) {
    if (node == null) {
      return null;
    }

    return fromExecutionDetails(node.getExecutionDetails());
  }

  static ExecutionDetailsInternalMetadata fromStateExecutionData(StateExecutionData stateExecutionData) {
    if (stateExecutionData == null) {
      return null;
    }

    return fromExecutionDetails(stateExecutionData.getExecutionDetails());
  }

  @VisibleForTesting
  static ExecutionDetailsInternalMetadata fromExecutionDetails(Object executionDetails) {
    if (executionDetails == null) {
      return null;
    }

    Map<String, ExecutionDataValue> executionDetailsMap = convertExecutionDetailsToMap(executionDetails);
    // The 3 methods below modify the execution details map and remove some keys. The execution map must be cleaned up
    // only after all these operations have run.
    TimingMetadata timing = TimingMetadata.extractFromExecutionDetails(executionDetailsMap);
    InstanceCountMetadata instanceCount = InstanceCountMetadata.extractFromExecutionDetails(executionDetailsMap);
    String activityId = extractActivityId(executionDetailsMap);

    // Now, clean up the execution details map and remove the other unnecessary keys.
    Map<String, Object> executionDetailsMapAfterCleanup = cleanedUpExecutionDetailsMap(executionDetailsMap);
    return ExecutionDetailsInternalMetadata.builder()
        .timing(timing)
        .instanceCount(instanceCount)
        .executionDetails(executionDetailsMapAfterCleanup)
        .activityId(activityId)
        .build();
  }

  @VisibleForTesting
  static Map<String, ExecutionDataValue> convertExecutionDetailsToMap(Object executionDetails) {
    // Return value should not be null.
    Map<String, ExecutionDataValue> finalExecutionDetailsMap = new HashMap<>();
    if (!(executionDetails instanceof Map)) {
      return finalExecutionDetailsMap;
    }

    Map<String, Object> executionDetailsMap = (Map<String, Object>) executionDetails;
    if (isEmpty(executionDetailsMap)) {
      return finalExecutionDetailsMap;
    }

    for (Map.Entry<String, Object> entry : executionDetailsMap.entrySet()) {
      if (entry.getValue() instanceof ExecutionDataValue) {
        finalExecutionDetailsMap.put(entry.getKey(), (ExecutionDataValue) entry.getValue());
      }
    }

    return finalExecutionDetailsMap;
  }

  @VisibleForTesting
  static Map<String, Object> cleanedUpExecutionDetailsMap(Map<String, ExecutionDataValue> executionDetailsMap) {
    if (isEmpty(executionDetailsMap)) {
      return null;
    }

    Map<String, Object> finalExecutionDetails = new HashMap<>();
    for (Map.Entry<String, ExecutionDataValue> entry : executionDetailsMap.entrySet()) {
      if (IGNORED_KEYS.contains(entry.getKey())) {
        continue;
      }

      ExecutionDataValue dataValue = entry.getValue();
      finalExecutionDetails.put(
          dataValue.getDisplayName() == null ? entry.getKey() : dataValue.getDisplayName(), dataValue.getValue());
    }

    return nullIfEmptyMap(finalExecutionDetails);
  }

  private static String extractActivityId(Map<String, ExecutionDataValue> executionDetailsMap) {
    if (isEmpty(executionDetailsMap) || !executionDetailsMap.containsKey(ACTIVITY_ID_KEY)) {
      return null;
    }

    ExecutionDataValue dataValue = executionDetailsMap.get(ACTIVITY_ID_KEY);
    executionDetailsMap.remove(ACTIVITY_ID_KEY);
    Object value = dataValue.getValue();
    if (!(value instanceof String)) {
      return null;
    }

    return (String) value;
  }
}

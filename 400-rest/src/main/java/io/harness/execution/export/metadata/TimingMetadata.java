/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.ExportExecutionsUtils;

import software.wings.api.ExecutionDataValue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class TimingMetadata {
  ZonedDateTime startTime;
  ZonedDateTime endTime;
  Duration duration;

  static TimingMetadata fromStartAndEndTimeObjects(Long startTime, Long endTime) {
    return fromStartAndEndTime(startTime == null ? 0 : startTime, endTime == null ? 0 : endTime);
  }

  static TimingMetadata fromStartAndEndTime(long startTime, long endTime) {
    if ((startTime <= 0 && endTime <= 0) || (endTime > 0 && endTime < startTime)) {
      return null;
    }

    TimingMetadataBuilder builder = TimingMetadata.builder();
    if (startTime > 0) {
      builder.startTime(ExportExecutionsUtils.prepareZonedDateTime(startTime));
    }
    if (endTime > 0) {
      builder.endTime(ExportExecutionsUtils.prepareZonedDateTime(endTime));
    }
    if (startTime > 0 && endTime > 0) {
      builder.duration(Duration.between(builder.startTime, builder.endTime));
    }

    return builder.build();
  }

  static TimingMetadata extractFromExecutionDetails(Map<String, ExecutionDataValue> executionDetailsMap) {
    if (isEmpty(executionDetailsMap)) {
      return null;
    }

    long startTime = epochMillisFromExecutionDetails(executionDetailsMap, "startTs");
    long endTime = epochMillisFromExecutionDetails(executionDetailsMap, "endTs");
    return TimingMetadata.fromStartAndEndTime(startTime, endTime);
  }

  private static long epochMillisFromExecutionDetails(Map<String, ExecutionDataValue> executionDetailsMap, String key) {
    if (!executionDetailsMap.containsKey(key)) {
      return -1;
    }

    ExecutionDataValue dataValue = executionDetailsMap.get(key);
    executionDetailsMap.remove(key);
    Object value = dataValue.getValue();
    if (value instanceof Long) {
      return (Long) value;
    } else if (value instanceof Integer) {
      return ((Integer) value).longValue();
    }

    return -1;
  }
}

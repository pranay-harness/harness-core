/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import software.wings.sm.StateExecutionData;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 12/9/16.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CloudWatchExecutionData extends StateExecutionData {
  private String region;
  private String namespace;
  private String metricName;
  private String percentile;
  private List<Dimension> dimensions = new ArrayList<>();
  private String timeDuration;
  private Datapoint datapoint;
  private String assertionStatement;
  private String assertionStatus;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "statistics",
        ExecutionDataValue.builder()
            .displayName("statistics")
            .value(datapoint == null ? null : datapoint.toString())
            .build());
    putNotNull(executionDetails, "assertionStatement",
        ExecutionDataValue.builder().displayName("Assertion").value(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        ExecutionDataValue.builder().displayName("Assertion Result").value(assertionStatus).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "region", ExecutionDataValue.builder().displayName("Region").value(region).build());
    putNotNull(
        executionDetails, "namespace", ExecutionDataValue.builder().displayName("Namespace").value(namespace).build());
    putNotNull(executionDetails, "metricName",
        ExecutionDataValue.builder().displayName("metricName").value(metricName).build());
    putNotNull(executionDetails, "percentile",
        ExecutionDataValue.builder().displayName("percentile").value(percentile).build());
    putNotNull(executionDetails, "dimensions",
        ExecutionDataValue.builder()
            .displayName("dimensions")
            .value(dimensions == null ? null : Lists.transform(dimensions, Functions.toStringFunction()))
            .build());
    putNotNull(executionDetails, "statistics",
        ExecutionDataValue.builder()
            .displayName("statistics")
            .value(datapoint == null ? null : datapoint.toString())
            .build());
    putNotNull(executionDetails, "assertion",
        ExecutionDataValue.builder().displayName("Assertion").value(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        ExecutionDataValue.builder().displayName("Assertion Result").value(assertionStatus).build());
    return executionDetails;
  }
}

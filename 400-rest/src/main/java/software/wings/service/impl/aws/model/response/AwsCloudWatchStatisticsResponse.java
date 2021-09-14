/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.aws.model.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;

import software.wings.service.impl.aws.model.AwsResponse;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class AwsCloudWatchStatisticsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private String label;
  private List<Datapoint> datapoints;

  @Builder
  public AwsCloudWatchStatisticsResponse(DelegateMetaInfo delegateMetaInfo, ExecutionStatus executionStatus,
      String errorMessage, String label, List<Datapoint> datapoints) {
    this.delegateMetaInfo = delegateMetaInfo;
    this.executionStatus = executionStatus;
    this.errorMessage = errorMessage;
    this.label = label;
    this.datapoints = datapoints;
  }
}

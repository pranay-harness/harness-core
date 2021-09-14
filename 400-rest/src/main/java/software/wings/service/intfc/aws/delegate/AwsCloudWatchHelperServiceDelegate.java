/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.aws.delegate;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.aws.model.request.AwsCloudWatchMetricDataRequest;
import software.wings.service.impl.aws.model.request.AwsCloudWatchStatisticsRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchMetricDataResponse;
import software.wings.service.impl.aws.model.response.AwsCloudWatchStatisticsResponse;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface AwsCloudWatchHelperServiceDelegate {
  // See https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_limits.html
  int MAX_QUERIES_PER_CALL = 500;

  @DelegateTaskType(TaskType.CLOUD_WATCH_GENERIC_METRIC_STATISTICS)
  AwsCloudWatchStatisticsResponse getMetricStatistics(AwsCloudWatchStatisticsRequest request);

  @DelegateTaskType(TaskType.CLOUD_WATCH_GENERIC_METRIC_DATA)
  AwsCloudWatchMetricDataResponse getMetricData(AwsCloudWatchMetricDataRequest request);
}

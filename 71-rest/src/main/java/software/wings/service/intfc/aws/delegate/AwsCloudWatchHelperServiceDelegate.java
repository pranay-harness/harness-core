package software.wings.service.intfc.aws.delegate;

import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.aws.model.request.AwsCloudWatchMetricDataRequest;
import software.wings.service.impl.aws.model.request.AwsCloudWatchStatisticsRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchMetricDataResponse;
import software.wings.service.impl.aws.model.response.AwsCloudWatchStatisticsResponse;

public interface AwsCloudWatchHelperServiceDelegate {
  @DelegateTaskType(TaskType.CLOUD_WATCH_GENERIC_METRIC_STATISTICS)
  AwsCloudWatchStatisticsResponse getMetricStatistics(AwsCloudWatchStatisticsRequest request);

  @DelegateTaskType(TaskType.CLOUD_WATCH_GENERIC_METRIC_DATA)
  AwsCloudWatchMetricDataResponse getMetricData(AwsCloudWatchMetricDataRequest request);
}

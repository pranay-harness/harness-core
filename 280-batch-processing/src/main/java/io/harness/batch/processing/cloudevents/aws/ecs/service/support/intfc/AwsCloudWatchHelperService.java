package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.request.AwsCloudWatchMetricDataRequest;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response.AwsCloudWatchMetricDataResponse;

public interface AwsCloudWatchHelperService {
  int MAX_QUERIES_PER_CALL = 500;

  AwsCloudWatchMetricDataResponse getMetricData(AwsCloudWatchMetricDataRequest request);
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.request.AwsCloudWatchMetricDataRequest;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response.AwsCloudWatchMetricDataResponse;

public interface AwsCloudWatchHelperService {
  int MAX_QUERIES_PER_CALL = 500;

  AwsCloudWatchMetricDataResponse getMetricData(AwsCloudWatchMetricDataRequest request);
}

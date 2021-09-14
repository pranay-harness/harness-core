/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.request;

import software.wings.beans.AwsCrossAccountAttributes;

import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import java.util.Collection;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

@Data
public class AwsCloudWatchMetricDataRequest {
  private AwsCrossAccountAttributes awsCrossAccountAttributes;
  private String region;
  private Date startTime;
  private Date endTime;
  private Collection<MetricDataQuery> metricDataQueries;

  @Builder
  private AwsCloudWatchMetricDataRequest(AwsCrossAccountAttributes awsCrossAccountAttributes, String region,
      Date startTime, Date endTime, Collection<MetricDataQuery> metricDataQueries) {
    this.awsCrossAccountAttributes = awsCrossAccountAttributes;
    this.region = region;
    this.startTime = startTime;
    this.endTime = endTime;
    this.metricDataQueries = metricDataQueries;
  }
}

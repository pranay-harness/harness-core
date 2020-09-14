package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.request;

import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsCrossAccountAttributes;

import java.util.Collection;
import java.util.Date;

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

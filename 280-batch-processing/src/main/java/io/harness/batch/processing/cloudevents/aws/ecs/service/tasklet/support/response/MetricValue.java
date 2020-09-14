package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class MetricValue {
  private String metricName;
  private String statistic;
  private List<Date> timestamps;
  private List<Double> values;
}

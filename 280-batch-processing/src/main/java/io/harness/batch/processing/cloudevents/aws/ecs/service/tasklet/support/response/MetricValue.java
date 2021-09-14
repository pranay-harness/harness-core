/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response;

import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricValue {
  private String metricName;
  private String statistic;
  private List<Date> timestamps;
  private List<Double> values;
}

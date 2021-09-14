/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsUtilizationData {
  private String clusterArn;
  private String clusterName;
  private String serviceArn;
  private String serviceName;
  private String clusterId;
  private String settingId;
  private List<MetricValue> metricValues;
}

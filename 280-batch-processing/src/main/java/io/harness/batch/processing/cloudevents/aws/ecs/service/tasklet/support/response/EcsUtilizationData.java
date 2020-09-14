package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

package io.harness.batch.processing.events.timeseries.data;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CostEventData {
  private String accountId;
  private String settingId;
  private String clusterId;
  private String clusterType;
  private String instanceId;
  private String instanceType;
  private String appId;
  private String serviceId;
  private String envId;
  private String cloudProviderId;
  private String deploymentId;
  private String cloudProvider;
  private String eventDescription;
  private String costEventType;
  private String costEventSource;
  private String namespace;
  private String workloadName;
  private String workloadType;
  private String cloudServiceName;
  private String taskId;
  private String launchType;

  private BigDecimal billingAmount;

  private long startTimestamp;
}

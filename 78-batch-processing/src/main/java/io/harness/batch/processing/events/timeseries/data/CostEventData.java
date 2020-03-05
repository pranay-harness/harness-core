package io.harness.batch.processing.events.timeseries.data;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CostEventData {
  String accountId;
  String settingId;
  String clusterId;
  String clusterType;
  String instanceId;
  String instanceType;
  String appId;
  String serviceId;
  String envId;
  String cloudProviderId;
  String deploymentId;
  String cloudProvider;
  String eventDescription;
  String costEventType;
  String costEventSource;
  String namespace;
  String workloadName;
  String workloadType;
  String cloudServiceName;
  String taskId;
  String launchType;

  BigDecimal billingAmount;

  long startTimestamp;

  String oldYamlRef;
  String newYamlRef;
}

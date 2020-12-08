package io.harness.batch.processing.ccm;

import io.harness.ccm.commons.beans.InstanceType;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanceEvent {
  String accountId;
  String cloudProviderId;
  String clusterId;
  String instanceId;
  String instanceName;
  Instant timestamp;
  EventType type;
  InstanceType instanceType;
  public enum EventType { STOP, START, UNKNOWN }
}

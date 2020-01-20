package io.harness.batch.processing.ccm;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

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
  public enum EventType { STOP, START }
}

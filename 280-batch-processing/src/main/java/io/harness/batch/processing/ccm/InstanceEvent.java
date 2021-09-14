/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

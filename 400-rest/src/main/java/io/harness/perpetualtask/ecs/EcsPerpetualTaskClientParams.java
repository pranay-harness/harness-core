/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.ecs;

import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class EcsPerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String region;
  private String settingId;
  private String clusterName;
  private String clusterId;
}

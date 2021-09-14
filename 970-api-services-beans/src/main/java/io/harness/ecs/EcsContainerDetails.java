/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ecs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsContainerDetails {
  private String taskId;
  private String taskArn;
  private String dockerId;
  private String completeDockerId;
  private String containerId;
  private String containerInstanceId;
  private String containerInstanceArn;
  private String ecsServiceName;
}

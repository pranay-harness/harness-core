/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

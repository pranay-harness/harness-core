/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.container;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ecs.EcsContainerDetails;

import com.amazonaws.services.ec2.model.Instance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by brett on 4/6/17
 */
@OwnedBy(CDP)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfo {
  public enum Status { SUCCESS, FAILURE }

  private String hostName;
  private String ip;
  private String containerId;
  private Instance ec2Instance;
  private Status status;
  private String podName;
  private String workloadName;
  private boolean newContainer;
  private EcsContainerDetails ecsContainerDetails;
  private boolean containerTasksReachable;

  /*
   * Helm Release to which the kubernetes pods belong to
   */
  private String releaseName;
  private String namespace;
}

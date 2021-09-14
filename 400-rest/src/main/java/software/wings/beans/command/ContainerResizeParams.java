/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import io.harness.delegate.beans.pcf.ResizeStrategy;

import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;

import java.util.List;
import lombok.Data;

@Data
public class ContainerResizeParams {
  private String clusterName;
  private int serviceSteadyStateTimeout;
  private boolean rollback;
  private boolean rollbackAllPhases;
  private String containerServiceName;
  private String image;
  private ResizeStrategy resizeStrategy;
  private boolean useFixedInstances;
  private int maxInstances;
  private int fixedInstances;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private Integer instanceCount;
  private InstanceUnitType instanceUnitType;
  private Integer downsizeInstanceCount;
  private InstanceUnitType downsizeInstanceUnitType;
  private List<String[]> originalServiceCounts;
  private List<String[]> originalTrafficWeights;
}

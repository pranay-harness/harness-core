package software.wings.beans.command;

import lombok.Data;
import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;

import java.util.List;

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

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
  private String containerServiceName;
  private ResizeStrategy resizeStrategy;
  private boolean useFixedInstances;
  private int maxInstances;
  private int fixedInstances;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
  private int instanceCount;
  private InstanceUnitType instanceUnitType;
}

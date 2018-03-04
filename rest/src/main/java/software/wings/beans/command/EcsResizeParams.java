package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcsResizeParams extends ContainerResizeParams {
  private String region;

  public static final class EcsResizeParamsBuilder {
    private String region;
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

    private EcsResizeParamsBuilder() {}

    public static EcsResizeParamsBuilder anEcsResizeParams() {
      return new EcsResizeParamsBuilder();
    }

    public EcsResizeParamsBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public EcsResizeParamsBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public EcsResizeParamsBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
      return this;
    }

    public EcsResizeParamsBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public EcsResizeParamsBuilder withContainerServiceName(String containerServiceName) {
      this.containerServiceName = containerServiceName;
      return this;
    }

    public EcsResizeParamsBuilder withResizeStrategy(ResizeStrategy resizeStrategy) {
      this.resizeStrategy = resizeStrategy;
      return this;
    }

    public EcsResizeParamsBuilder withUseFixedInstances(boolean useFixedInstances) {
      this.useFixedInstances = useFixedInstances;
      return this;
    }

    public EcsResizeParamsBuilder withMaxInstances(int maxInstances) {
      this.maxInstances = maxInstances;
      return this;
    }

    public EcsResizeParamsBuilder withFixedInstances(int fixedInstances) {
      this.fixedInstances = fixedInstances;
      return this;
    }

    public EcsResizeParamsBuilder withNewInstanceData(List<ContainerServiceData> newInstanceData) {
      this.newInstanceData = newInstanceData;
      return this;
    }

    public EcsResizeParamsBuilder withOldInstanceData(List<ContainerServiceData> oldInstanceData) {
      this.oldInstanceData = oldInstanceData;
      return this;
    }

    public EcsResizeParamsBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public EcsResizeParamsBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public EcsResizeParamsBuilder but() {
      return anEcsResizeParams()
          .withRegion(region)
          .withClusterName(clusterName)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
          .withRollback(rollback)
          .withContainerServiceName(containerServiceName)
          .withResizeStrategy(resizeStrategy)
          .withUseFixedInstances(useFixedInstances)
          .withMaxInstances(maxInstances)
          .withFixedInstances(fixedInstances)
          .withNewInstanceData(newInstanceData)
          .withOldInstanceData(oldInstanceData)
          .withInstanceCount(instanceCount)
          .withInstanceUnitType(instanceUnitType);
    }

    public EcsResizeParams build() {
      EcsResizeParams ecsResizeParams = new EcsResizeParams();
      ecsResizeParams.setRegion(region);
      ecsResizeParams.setClusterName(clusterName);
      ecsResizeParams.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      ecsResizeParams.setRollback(rollback);
      ecsResizeParams.setContainerServiceName(containerServiceName);
      ecsResizeParams.setResizeStrategy(resizeStrategy);
      ecsResizeParams.setUseFixedInstances(useFixedInstances);
      ecsResizeParams.setMaxInstances(maxInstances);
      ecsResizeParams.setFixedInstances(fixedInstances);
      ecsResizeParams.setNewInstanceData(newInstanceData);
      ecsResizeParams.setOldInstanceData(oldInstanceData);
      ecsResizeParams.setInstanceCount(instanceCount);
      ecsResizeParams.setInstanceUnitType(instanceUnitType);
      return ecsResizeParams;
    }
  }
}

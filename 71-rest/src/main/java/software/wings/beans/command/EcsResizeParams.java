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

    public EcsResizeParamsBuilder withRollbackAllPhases(boolean rollbackAllPhases) {
      this.rollbackAllPhases = rollbackAllPhases;
      return this;
    }

    public EcsResizeParamsBuilder withContainerServiceName(String containerServiceName) {
      this.containerServiceName = containerServiceName;
      return this;
    }

    public EcsResizeParamsBuilder withImage(String image) {
      this.image = image;
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

    public EcsResizeParamsBuilder withInstanceCount(Integer instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public EcsResizeParamsBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public EcsResizeParamsBuilder withDownsizeInstanceCount(Integer downsizeInstanceCount) {
      this.downsizeInstanceCount = downsizeInstanceCount;
      return this;
    }

    public EcsResizeParamsBuilder withDownsizeInstanceUnitType(InstanceUnitType downsizeInstanceUnitType) {
      this.downsizeInstanceUnitType = downsizeInstanceUnitType;
      return this;
    }

    public EcsResizeParamsBuilder withOriginalServiceCounts(List<String[]> originalServiceCounts) {
      this.originalServiceCounts = originalServiceCounts;
      return this;
    }

    public EcsResizeParamsBuilder withOriginalTrafficWeights(List<String[]> originalTrafficWeights) {
      this.originalTrafficWeights = originalTrafficWeights;
      return this;
    }

    public EcsResizeParamsBuilder but() {
      return anEcsResizeParams()
          .withRegion(region)
          .withClusterName(clusterName)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
          .withRollback(rollback)
          .withRollbackAllPhases(rollbackAllPhases)
          .withContainerServiceName(containerServiceName)
          .withImage(image)
          .withResizeStrategy(resizeStrategy)
          .withUseFixedInstances(useFixedInstances)
          .withMaxInstances(maxInstances)
          .withFixedInstances(fixedInstances)
          .withNewInstanceData(newInstanceData)
          .withOldInstanceData(oldInstanceData)
          .withInstanceCount(instanceCount)
          .withInstanceUnitType(instanceUnitType)
          .withDownsizeInstanceCount(downsizeInstanceCount)
          .withDownsizeInstanceUnitType(downsizeInstanceUnitType)
          .withOriginalServiceCounts(originalServiceCounts)
          .withOriginalTrafficWeights(originalTrafficWeights);
    }

    public EcsResizeParams build() {
      EcsResizeParams ecsResizeParams = new EcsResizeParams();
      ecsResizeParams.setRegion(region);
      ecsResizeParams.setClusterName(clusterName);
      ecsResizeParams.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      ecsResizeParams.setRollback(rollback);
      ecsResizeParams.setRollbackAllPhases(rollbackAllPhases);
      ecsResizeParams.setContainerServiceName(containerServiceName);
      ecsResizeParams.setImage(image);
      ecsResizeParams.setResizeStrategy(resizeStrategy);
      ecsResizeParams.setUseFixedInstances(useFixedInstances);
      ecsResizeParams.setMaxInstances(maxInstances);
      ecsResizeParams.setFixedInstances(fixedInstances);
      ecsResizeParams.setNewInstanceData(newInstanceData);
      ecsResizeParams.setOldInstanceData(oldInstanceData);
      ecsResizeParams.setInstanceCount(instanceCount);
      ecsResizeParams.setInstanceUnitType(instanceUnitType);
      ecsResizeParams.setDownsizeInstanceCount(downsizeInstanceCount);
      ecsResizeParams.setDownsizeInstanceUnitType(downsizeInstanceUnitType);
      ecsResizeParams.setOriginalServiceCounts(originalServiceCounts);
      ecsResizeParams.setOriginalTrafficWeights(originalTrafficWeights);
      return ecsResizeParams;
    }
  }
}

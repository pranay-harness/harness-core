package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.substringBefore;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.Service;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentType;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResizeCommandUnit extends ContainerResizeCommandUnit {
  @Inject @Transient private transient AwsClusterService awsClusterService;

  public ResizeCommandUnit() {
    super(CommandUnitType.RESIZE);
    setDeploymentType(DeploymentType.ECS.name());
  }

  @Override
  protected List<ContainerInfo> executeResize(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    EcsResizeParams resizeParams = (EcsResizeParams) contextData.resizeParams;
    return awsClusterService.resizeCluster(resizeParams.getRegion(), contextData.settingAttribute,
        contextData.encryptedDataDetails, resizeParams.getClusterName(), containerServiceData.getName(),
        containerServiceData.getPreviousCount(), containerServiceData.getDesiredCount(),
        resizeParams.getServiceSteadyStateTimeout(), executionLogCallback);
  }

  @Override
  protected Map<String, Integer> getActiveServiceCounts(ContextData contextData) {
    EcsResizeParams resizeParams = (EcsResizeParams) contextData.resizeParams;
    return awsClusterService.getActiveServiceCounts(resizeParams.getRegion(), contextData.settingAttribute,
        contextData.encryptedDataDetails, resizeParams.getClusterName(), resizeParams.getContainerServiceName());
  }

  @Override
  protected Map<String, String> getActiveServiceImages(ContextData contextData) {
    EcsResizeParams resizeParams = (EcsResizeParams) contextData.resizeParams;
    String imagePrefix = substringBefore(contextData.resizeParams.getImage(), ":");
    return awsClusterService.getActiveServiceImages(resizeParams.getRegion(), contextData.settingAttribute,
        contextData.encryptedDataDetails, resizeParams.getClusterName(), resizeParams.getContainerServiceName(),
        imagePrefix);
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(ContextData contextData) {
    EcsResizeParams resizeParams = (EcsResizeParams) contextData.resizeParams;
    Optional<Service> service = awsClusterService
                                    .getServices(resizeParams.getRegion(), contextData.settingAttribute,
                                        contextData.encryptedDataDetails, resizeParams.getClusterName())
                                    .stream()
                                    .filter(svc -> svc.getServiceName().equals(resizeParams.getContainerServiceName()))
                                    .findFirst();
    return service.map(Service::getDesiredCount);
  }

  @Override
  protected Map<String, Integer> getTrafficWeights(ContextData contextData) {
    return new HashMap<>();
  }

  @Override
  protected int getPreviousTrafficPercent(ContextData contextData) {
    return 0;
  }

  @Override
  protected Integer getDesiredTrafficPercent(ContextData contextData) {
    return 0;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("RESIZE")
  public static class Yaml extends ContainerResizeCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.RESIZE.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.RESIZE.name(), deploymentType);
    }
  }
}

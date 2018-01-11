package software.wings.beans.command;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentType;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class ResizeCommandUnit extends ContainerResizeCommandUnit {
  @Inject @Transient protected transient AwsClusterService awsClusterService;

  public ResizeCommandUnit() {
    super(CommandUnitType.RESIZE);
    setDeploymentType(DeploymentType.ECS.name());
  }

  @Override
  protected List<ContainerInfo> executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerResizeParams params,
      ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    EcsResizeParams resizeParams = (EcsResizeParams) params;
    return awsClusterService.resizeCluster(resizeParams.getRegion(), cloudProviderSetting, encryptedDataDetails,
        resizeParams.getClusterName(), containerServiceData.getName(), containerServiceData.getPreviousCount(),
        containerServiceData.getDesiredCount(), resizeParams.getServiceSteadyStateTimeout(), executionLogCallback);
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

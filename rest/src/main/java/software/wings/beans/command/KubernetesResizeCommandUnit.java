package software.wings.beans.command;

import static java.util.Collections.emptyList;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentType;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesResizeCommandUnit extends ContainerResizeCommandUnit {
  @Inject @Transient private transient GkeClusterService gkeClusterService;

  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;

  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNETES);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected List<ContainerInfo> executeInternal(SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> edd,
      ContainerResizeParams params, ContainerServiceData containerServiceData,
      ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) params;
    KubernetesConfig kubernetesConfig;
    List<EncryptedDataDetail> encryptedDataDetails;
    if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
      encryptedDataDetails = edd;
    } else {
      kubernetesConfig = gkeClusterService.getCluster(
          cloudProviderSetting, edd, resizeParams.getClusterName(), resizeParams.getNamespace());
      kubernetesConfig.setDecrypted(true);
      encryptedDataDetails = emptyList();
    }
    return kubernetesContainerService.setControllerPodCount(kubernetesConfig, encryptedDataDetails,
        resizeParams.getClusterName(), containerServiceData.getName(), containerServiceData.getPreviousCount(),
        containerServiceData.getDesiredCount(), resizeParams.getServiceSteadyStateTimeout(), executionLogCallback);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("RESIZE_KUBERNETES")
  public static class Yaml extends ContainerResizeCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.RESIZE_KUBERNETES.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.RESIZE_KUBERNETES.name(), deploymentType);
    }
  }
}

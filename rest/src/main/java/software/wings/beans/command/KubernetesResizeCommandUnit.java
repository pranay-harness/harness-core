package software.wings.beans.command;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;

import java.util.List;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesResizeCommandUnit extends ContainerOrchestrationCommandUnit {
  @Inject @Transient protected transient GkeClusterService gkeClusterService;

  @Inject @Transient protected transient KubernetesContainerService kubernetesContainerService;

  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNETES);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected List<ContainerInfo> executeInternal(String region, SettingAttribute cloudProviderSetting,
      String clusterName, String serviceName, int previousCount, int desiredCount,
      ExecutionLogCallback executionLogCallback) {
    KubernetesConfig kubernetesConfig;
    if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
    } else {
      kubernetesConfig = gkeClusterService.getCluster(cloudProviderSetting, clusterName);
    }
    return kubernetesContainerService.setControllerPodCount(
        kubernetesConfig, clusterName, serviceName, previousCount, desiredCount, executionLogCallback);
  }
}

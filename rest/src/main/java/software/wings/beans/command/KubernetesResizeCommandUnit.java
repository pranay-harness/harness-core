package software.wings.beans.command;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
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
public class KubernetesResizeCommandUnit extends ContainerOrchestrationCommandUnit {
  @Inject @Transient protected transient GkeClusterService gkeClusterService;

  @Inject @Transient protected transient KubernetesContainerService kubernetesContainerService;

  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNETES);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected List<ContainerInfo> executeInternal(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String namespace, String serviceName,
      int previousCount, int desiredCount, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    KubernetesConfig kubernetesConfig;
    if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
    } else {
      kubernetesConfig =
          gkeClusterService.getCluster(cloudProviderSetting, encryptedDataDetails, clusterName, namespace);
    }
    return kubernetesContainerService.setControllerPodCount(kubernetesConfig, encryptedDataDetails, clusterName,
        serviceName, previousCount, desiredCount, executionLogCallback);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("RESIZE_KUBERNETES")
  public static class Yaml extends ContainerOrchestrationCommandUnit.Yaml {
    public Yaml() {
      super();
      setCommandUnitType(CommandUnitType.RESIZE_KUBERNETES.name());
    }

    public static final class Builder extends ContainerOrchestrationCommandUnit.Yaml.Builder {
      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      @Override
      protected Yaml getCommandUnitYaml() {
        return new KubernetesResizeCommandUnit.Yaml();
      }
    }
  }
}

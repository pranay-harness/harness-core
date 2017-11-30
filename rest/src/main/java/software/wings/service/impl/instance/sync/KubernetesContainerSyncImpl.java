package software.wings.service.impl.instance.sync;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo.Builder;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.instance.sync.request.ContainerFilter;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Validator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 9/6/17
 */
public class KubernetesContainerSyncImpl implements ContainerSync {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesContainerSyncImpl.class);

  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private SettingsService settingsService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private GkeClusterService gkeClusterService;
  @Inject private SecretManager secretManager;

  @Override
  public ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest) {
    ContainerFilter filter = syncRequest.getFilter();
    Collection<ContainerDeploymentInfo> containerDeploymentInfoCollection =
        filter.getContainerDeploymentInfoCollection();
    List<ContainerInfo> result = Lists.newArrayList();

    for (ContainerDeploymentInfo containerDeploymentInfo : containerDeploymentInfoCollection) {
      try {
        InfrastructureMapping infrastructureMapping =
            infraMappingService.get(containerDeploymentInfo.getAppId(), containerDeploymentInfo.getInfraMappingId());
        Validator.notNullCheck("InfrastructureMapping is null for id:" + containerDeploymentInfo.getInfraMappingId(),
            infrastructureMapping);

        SettingAttribute settingAttribute = infrastructureMapping instanceof DirectKubernetesInfrastructureMapping
            ? aSettingAttribute()
                  .withValue(((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig())
                  .build()
            : settingsService.get(infrastructureMapping.getComputeProviderSettingId());

        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(),
                infrastructureMapping.getAppId(), containerDeploymentInfo.getWorkflowExecutionId());

        KubernetesConfig kubernetesConfig = getKubernetesConfig(infrastructureMapping, encryptionDetails);
        Validator.notNullCheck(
            "KubernetesConfig is null for given infraMappingId:" + containerDeploymentInfo.getInfraMappingId(),
            infrastructureMapping);

        // TODO(brett) - sync task?

        ReplicationController replicationController = kubernetesContainerService.getController(
            kubernetesConfig, encryptionDetails, containerDeploymentInfo.getContainerSvcName());

        if (replicationController != null) {
          Map<String, String> labels = replicationController.getMetadata().getLabels();
          List<Service> services =
              kubernetesContainerService.getServices(kubernetesConfig, Collections.emptyList(), labels).getItems();
          String serviceName = services.size() > 0 ? services.get(0).getMetadata().getName() : "None";
          for (Pod pod :
              kubernetesContainerService.getPods(kubernetesConfig, Collections.emptyList(), labels).getItems()) {
            if (pod.getStatus().getPhase().equals("Running")) {
              List<ReplicationController> rcs =
                  kubernetesContainerService
                      .getControllers(kubernetesConfig, Collections.emptyList(), pod.getMetadata().getLabels())
                      .getItems();
              String rcName = rcs.size() > 0 ? rcs.get(0).getMetadata().getName() : "None";

              KubernetesContainerInfo kubernetesContainerInfo =
                  Builder.aKubernetesContainerInfo()
                      .withClusterName(containerDeploymentInfo.getClusterName())
                      .withPodName(pod.getMetadata().getName())
                      .withReplicationControllerName(rcName)
                      .withServiceName(serviceName)
                      .build();
              result.add(kubernetesContainerInfo);
            }
          }
        }
      } catch (Exception ex) {
        logger.warn("Error while getting instances for container", ex);
      }
    }
    return ContainerSyncResponse.builder().containerInfoList(result).build();
  }

  private KubernetesConfig getKubernetesConfig(
      InfrastructureMapping infrastructureMapping, List<EncryptedDataDetail> encryptionDetails) {
    if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      GcpKubernetesInfrastructureMapping gcpInfraMapping = (GcpKubernetesInfrastructureMapping) infrastructureMapping;
      SettingAttribute computeProviderSetting =
          settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      return gkeClusterService.getCluster(
          computeProviderSetting, encryptionDetails, gcpInfraMapping.getClusterName(), gcpInfraMapping.getNamespace());
    } else {
      return ((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig();
    }
  }
}

package software.wings.service.impl.instance;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerDeploymentEvent;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.EcsContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.KubernetesContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.impl.instance.sync.InstanceSyncService;
import software.wings.service.impl.instance.sync.request.EcsFilter;
import software.wings.service.impl.instance.sync.request.InstanceFilter;
import software.wings.service.impl.instance.sync.request.InstanceSyncRequest;
import software.wings.service.impl.instance.sync.request.KubernetesFilter;
import software.wings.service.impl.instance.sync.response.InstanceSyncResponse;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.utils.Validator;

import java.util.List;

/**
 * Receives all the completed phases and their container info and fetches from the .
 * The instance information is used in the service and infrastructure dashboards.
 * @author rktummala on 08/24/17
 *
 * For sender information,
 * @see software.wings.beans.CanaryWorkflowExecutionAdvisor
 */
public class ContainerDeploymentEventListener extends AbstractQueueListener<ContainerDeploymentEvent> {
  private static final Logger logger = LoggerFactory.getLogger(ContainerDeploymentEventListener.class);
  @Inject private InstanceService instanceService;
  @Inject @Named("KubernetesInstanceSync") private InstanceSyncService kubernetesSyncService;
  @Inject @Named("EcsInstanceSync") private InstanceSyncService ecsSyncService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private GkeClusterService gkeClusterService;
  @Inject private SettingsService settingsService;

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(ContainerDeploymentEvent containerDeploymentEvent) throws Exception {
    try {
      Validator.notNullCheck("ContainerDeploymentEvent", containerDeploymentEvent);
      ContainerDeploymentInfo containerDeploymentInfo = containerDeploymentEvent.getContainerDeploymentInfo();

      String key = getKeyForSynchronization(containerDeploymentInfo);
      Validator.notNullCheck("KeyForSync", key);

      synchronized (key) {
        InstanceSyncResponse instanceSyncResponse = getLatestInstances(containerDeploymentInfo);
        Validator.notNullCheck("InstanceSyncResponse", instanceSyncResponse);

        List<ContainerInfo> containerInfoList = instanceSyncResponse.getContainerInfoList();

        instanceService.buildAndSaveInstances(containerDeploymentInfo, containerInfoList);
      }

    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      logger.error("Exception while processing phase completion event.", ex);
    }
  }

  private InstanceSyncResponse getLatestInstances(ContainerDeploymentInfo containerDeploymentInfo) {
    InstanceFilter instanceFilter = getInstanceFilter(containerDeploymentInfo);
    Validator.notNullCheck("InstanceFilter", instanceFilter);

    InstanceSyncRequest instanceSyncRequest =
        InstanceSyncRequest.Builder.anInstanceSyncRequest().withFilter(instanceFilter).build();

    if (containerDeploymentInfo instanceof KubernetesContainerDeploymentInfo) {
      return kubernetesSyncService.getInstances(instanceSyncRequest);
    } else if (containerDeploymentInfo instanceof EcsContainerDeploymentInfo) {
      return ecsSyncService.getInstances(instanceSyncRequest);
    }

    return null;
  }

  private InstanceFilter getInstanceFilter(ContainerDeploymentInfo containerDeploymentInfo) {
    InstanceFilter instanceFilter = null;

    if (containerDeploymentInfo instanceof KubernetesContainerDeploymentInfo) {
      KubernetesContainerDeploymentInfo kubernetesContainerDeploymentInfo =
          (KubernetesContainerDeploymentInfo) containerDeploymentInfo;
      instanceFilter =
          KubernetesFilter.Builder.aKubernetesFilter()
              .withClusterName(containerDeploymentInfo.getClusterName())
              .withReplicationControllerNameList(kubernetesContainerDeploymentInfo.getReplicationControllerNameList())
              .withKubernetesConfig(getKubernetesConfig(containerDeploymentInfo))
              .build();

    } else if (containerDeploymentInfo instanceof EcsContainerDeploymentInfo) {
      EcsContainerDeploymentInfo ecsContainerDeploymentInfo = (EcsContainerDeploymentInfo) containerDeploymentInfo;
      instanceFilter = EcsFilter.Builder.anEcsFilter()
                           .withClusterName(containerDeploymentInfo.getClusterName())
                           .withServiceNameList(ecsContainerDeploymentInfo.getEcsServiceNameList())
                           .withAwsComputeProviderId(containerDeploymentInfo.getComputeProviderId())
                           .withRegion(ecsContainerDeploymentInfo.getAwsRegion())
                           .build();
    }

    return instanceFilter;
  }

  private String getKeyForSynchronization(ContainerDeploymentInfo containerDeploymentInfo) {
    InstanceFilter instanceFilter = null;

    if (containerDeploymentInfo instanceof KubernetesContainerDeploymentInfo) {
      KubernetesContainerDeploymentInfo kubernetesContainerDeploymentInfo =
          (KubernetesContainerDeploymentInfo) containerDeploymentInfo;
      List<String> replicationControllerNameList = kubernetesContainerDeploymentInfo.getReplicationControllerNameList();
      if (replicationControllerNameList != null && !replicationControllerNameList.isEmpty()) {
        return kubernetesContainerDeploymentInfo.getRcNameWithoutRevision(replicationControllerNameList.get(0));
      }

    } else if (containerDeploymentInfo instanceof EcsContainerDeploymentInfo) {
      EcsContainerDeploymentInfo ecsContainerDeploymentInfo = (EcsContainerDeploymentInfo) containerDeploymentInfo;
      List<String> ecsServiceNameList = ecsContainerDeploymentInfo.getEcsServiceNameList();
      if (ecsServiceNameList != null && !ecsServiceNameList.isEmpty()) {
        return ecsContainerDeploymentInfo.getTaskDefinitionFamilyName(ecsServiceNameList.get(0));
      }
    }

    return null;
  }

  private KubernetesConfig getKubernetesConfig(ContainerDeploymentInfo containerDeploymentInfo) {
    KubernetesConfig kubernetesConfig;
    InfrastructureMapping infrastructureMapping =
        infraMappingService.get(containerDeploymentInfo.getAppId(), containerDeploymentInfo.getInfraMappingId());
    String clusterName;
    if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      clusterName = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getClusterName();
      // TODO check with brett if this case is applicable here
      //      if (Constants.RUNTIME.equals(clusterName)) {
      //        clusterName = getClusterElement(context).getName();
      //      }

      SettingAttribute computeProviderSetting =
          settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      kubernetesConfig = gkeClusterService.getCluster(computeProviderSetting, clusterName);
    } else {
      kubernetesConfig = ((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig();
    }

    return kubernetesConfig;
  }
}

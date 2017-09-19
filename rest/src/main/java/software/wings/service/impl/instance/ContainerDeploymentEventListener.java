package software.wings.service.impl.instance;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerDeploymentEvent;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
  @Inject private ContainerInstanceHelper containerInstanceHelper;
  @Inject private InstanceService instanceService;

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(ContainerDeploymentEvent containerDeploymentEvent) throws Exception {
    try {
      Validator.notNullCheck("ContainerDeploymentEvent", containerDeploymentEvent);
      synchronized (containerDeploymentEvent.getContainerSvcNameNoRevision()) {
        long syncTimestamp = System.currentTimeMillis();
        InstanceType instanceType = containerDeploymentEvent.getInstanceType();
        String appId = containerDeploymentEvent.getAppId();
        String infraMappingId = containerDeploymentEvent.getInfraMappingId();
        String clusterName = containerDeploymentEvent.getClusterName();
        String computeProviderId = containerDeploymentEvent.getComputeProviderId();
        String containerSvcNameNoRevision = containerDeploymentEvent.getContainerSvcNameNoRevision();
        Set<String> containerSvcNameSet = containerDeploymentEvent.getContainerSvcNameSet();

        Map<String, ContainerDeploymentInfo> containerSvcNameDeploymentInfoMap = Maps.newHashMap();

        List<ContainerDeploymentInfo> currentContainerDeploymentsInDB =
            instanceService.getContainerDeploymentInfoList(containerSvcNameNoRevision, appId);

        currentContainerDeploymentsInDB.stream().forEach(currentContainerDeploymentInDB
            -> containerSvcNameDeploymentInfoMap.put(
                currentContainerDeploymentInDB.getContainerSvcName(), currentContainerDeploymentInDB));

        for (String containerSvcName : containerSvcNameSet) {
          ContainerDeploymentInfo containerDeploymentInfo = containerSvcNameDeploymentInfoMap.get(containerSvcName);
          if (containerDeploymentInfo == null) {
            containerDeploymentInfo = containerInstanceHelper.buildContainerDeploymentInfo(
                containerSvcName, containerDeploymentEvent, syncTimestamp);
            containerSvcNameDeploymentInfoMap.put(
                containerDeploymentInfo.getContainerSvcName(), containerDeploymentInfo);
          }
        }

        // This includes the service names that were involved in the event update and the ones in the db
        ContainerSyncResponse instanceSyncResponse =
            containerInstanceHelper.getLatestInstancesFromCloud(containerSvcNameDeploymentInfoMap.keySet(),
                instanceType, appId, infraMappingId, clusterName, computeProviderId);
        Validator.notNullCheck("InstanceSyncResponse", instanceSyncResponse);

        List<ContainerInfo> containerInfoList = instanceSyncResponse.getContainerInfoList();

        if (containerInfoList == null) {
          containerInfoList = Lists.newArrayList();
        }

        // Even though the containerInfoList is empty, we still run through this method since it also deletes the
        // revisions that don't have any instances
        containerInstanceHelper.buildAndSaveInstancesFromContainerInfo(
            containerSvcNameDeploymentInfoMap, containerInfoList, containerSvcNameNoRevision, instanceType, appId);

        containerInstanceHelper.buildAndSaveContainerDeploymentInfo(
            containerSvcNameNoRevision, containerSvcNameDeploymentInfoMap.values(), appId, instanceType, syncTimestamp);
      }

    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      logger.error("Exception while processing phase completion event.", ex);
    }
  }
}

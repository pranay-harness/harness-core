package software.wings.scheduler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.service.impl.instance.ContainerInstanceHelper;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/**
 * Periodic job that syncs for instances with the current containers like kubernetes and ECS.
 * @author rktummala on 09/14/17
 */
public class ContainerSyncJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ContainerSyncJob.class);
  // we don't have to process the instances that we have processed less than one hour
  private int INTERVAL = 3600000;

  @Inject private InstanceService instanceService;
  @Inject private ContainerInstanceHelper containerInstanceHelper;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String appId = jobExecutionContext.getMergedJobDataMap().getString("appId");

    Set<String> containerSvcNameNoRevisionSet =
        instanceService.getLeastRecentVisitedContainerDeployments(appId, System.currentTimeMillis() - INTERVAL);
    if (containerSvcNameNoRevisionSet == null || containerSvcNameNoRevisionSet.isEmpty()) {
      return;
    }

    final Map<String, ContainerDeploymentInfo> containerSvcNameDeploymentInfoMap = Maps.newHashMap();

    for (String containerSvcNameNoRevision : containerSvcNameNoRevisionSet) {
      List<ContainerDeploymentInfo> containerDeploymentInfoList =
          instanceService.getContainerDeploymentInfoList(containerSvcNameNoRevision, appId);
      containerDeploymentInfoList.stream().forEach(containerDeploymentInfo
          -> containerSvcNameDeploymentInfoMap.put(
              containerDeploymentInfo.getContainerSvcName(), containerDeploymentInfo));

      // containerDeploymentInfoList.get(0) is passed to get the information that is common to all the deployments
      // queried by appId and containerSvcNameNoRevision.
      buildContainerInstances(containerSvcNameDeploymentInfoMap, containerDeploymentInfoList.get(0));
    }
  }

  private void buildContainerInstances(Map<String, ContainerDeploymentInfo> containerSvcNameDeploymentInfoMap,
      ContainerDeploymentInfo containerDeploymentInfo) {
    // common attributes for all the instances belonging to the same containerSvcNameNoRevision.
    // The workflow and stateExecutionInstanceId and other attributes might be different
    String appId = containerDeploymentInfo.getAppId();
    InstanceType instanceType = containerDeploymentInfo.getInstanceType();
    String infraMappingId = containerDeploymentInfo.getInfraMappingId();
    String clusterName = containerDeploymentInfo.getClusterName();
    String computeProviderId = containerDeploymentInfo.getComputeProviderId();
    String containerSvcNameNoRevision = containerDeploymentInfo.getContainerSvcNameNoRevision();

    Set<String> containerServiceNameSet = containerSvcNameDeploymentInfoMap.keySet();

    ContainerSyncResponse instanceSyncResponse = containerInstanceHelper.getLatestInstancesFromCloud(
        containerServiceNameSet, instanceType, appId, infraMappingId, clusterName, computeProviderId);

    Validator.notNullCheck("InstanceSyncResponse", instanceSyncResponse);

    List<ContainerInfo> containerInfoList = instanceSyncResponse.getContainerInfoList();

    if (containerInfoList == null) {
      containerInfoList = Lists.newArrayList();
    }

    // Even though the containerInfoList is empty, we still run through this method since it also deletes the revisions
    // that don't have any instances
    containerInstanceHelper.buildAndSaveInstancesFromContainerInfo(
        containerSvcNameDeploymentInfoMap, containerInfoList, containerSvcNameNoRevision, instanceType, appId);
  }
}

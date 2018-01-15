package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.ContainerInstanceHelper;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * Periodic job that syncs for instances with the current containers like kubernetes and ECS.
 *
 * @author rktummala on 09/14/17
 */
public class ContainerSyncJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ContainerSyncJob.class);

  public static final String APP_ID_KEY = "appId";

  // we don't have to process the instances that we have processed less than one hour
  private int SYNC_INTERVAL = 3600000;

  public static final String GROUP = "CONTAINER_SYNC_CRON_GROUP";
  private static final int POLL_INTERVAL = 600;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private InstanceService instanceService;
  @Inject private ContainerInstanceHelper containerInstanceHelper;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static void add(QuartzScheduler jobScheduler, String appId) {
    jobScheduler.deleteJob(appId, GROUP);

    JobDetail job =
        JobBuilder.newJob(ContainerSyncJob.class).withIdentity(appId, GROUP).usingJobData(APP_ID_KEY, appId).build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(appId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    logger.info("Executing container sync job for appId:" + appId);

    Set<String> containerSvcNameNoRevisionSet =
        instanceService.getLeastRecentSyncedContainerDeployments(appId, System.currentTimeMillis() - SYNC_INTERVAL);
    if (isEmpty(containerSvcNameNoRevisionSet)) {
      logger.info("No Container deployments to process for appId:" + appId);
      // This is making the job self pruning. This allow to simplify the logic in deletion of the application.
      Application application = wingsPersistence.get(Application.class, appId);
      if (application == null) {
        jobScheduler.deleteJob(appId, GROUP);
      }

      return;
    }

    for (String containerSvcNameNoRevision : containerSvcNameNoRevisionSet) {
      final Map<String, ContainerDeploymentInfo> containerSvcNameDeploymentInfoMap = Maps.newHashMap();
      List<ContainerDeploymentInfo> containerDeploymentInfoList =
          instanceService.getContainerDeploymentInfoList(containerSvcNameNoRevision, appId);
      containerDeploymentInfoList.forEach(containerDeploymentInfo
          -> containerSvcNameDeploymentInfoMap.put(
              containerDeploymentInfo.getContainerSvcName(), containerDeploymentInfo));

      // containerDeploymentInfoList.get(0) is passed to get the information that is common to all the deployments that
      // belong to the same containerSvcNameNoRevision.
      syncContainerInstances(containerSvcNameDeploymentInfoMap, containerDeploymentInfoList.get(0));
    }
    logger.info("Container sync done for appId:" + appId);
  }

  private void syncContainerInstances(Map<String, ContainerDeploymentInfo> containerSvcNameDeploymentInfoMap,
      ContainerDeploymentInfo containerDeploymentInfo) {
    // common attributes for all the instances belonging to the same containerSvcNameNoRevision.
    // The workflow and stateExecutionInstanceId and other attributes might be different
    String appId = containerDeploymentInfo.getAppId();
    InstanceType instanceType = containerDeploymentInfo.getInstanceType();
    String containerSvcNameNoRevision = containerDeploymentInfo.getContainerSvcNameNoRevision();

    ContainerSyncResponse instanceSyncResponse = containerInstanceHelper.getLatestInstancesFromContainerServer(
        containerSvcNameDeploymentInfoMap.values(), instanceType);

    Validator.notNullCheck("InstanceSyncResponse", instanceSyncResponse);

    List<ContainerInfo> containerInfoList = instanceSyncResponse.getContainerInfoList();

    if (containerInfoList == null) {
      containerInfoList = Lists.newArrayList();
    }

    // Even though the containerInfoList is empty, we still run through this method since it also deletes the revisions
    // that don't have any instances
    containerInstanceHelper.updateInstancesFromContainerInfo(
        containerSvcNameDeploymentInfoMap, containerInfoList, containerSvcNameNoRevision, instanceType, appId);
  }
}

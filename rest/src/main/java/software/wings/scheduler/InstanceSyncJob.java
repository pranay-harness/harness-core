package software.wings.scheduler;

import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

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
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.impl.instance.InstanceHandler;
import software.wings.service.impl.instance.InstanceHandlerFactory;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.utils.Util;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Periodic job that syncs for instances for all the deployment types except Physical data center.
 *
 * @author rktummala on 09/14/17
 */
public class InstanceSyncJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(InstanceSyncJob.class);

  public static final String APP_ID_KEY = "appId";

  public static final String GROUP = "INSTANCE_SYNC_CRON_GROUP";
  private static final int POLL_INTERVAL = 600;

  @Inject private InstanceHandlerFactory instanceHandlerFactory;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private WingsPersistence wingsPersistence;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;

  public static void add(QuartzScheduler jobScheduler, String appId) {
    jobScheduler.deleteJob(appId, GROUP);

    JobDetail job =
        JobBuilder.newJob(InstanceSyncJob.class).withIdentity(appId, GROUP).usingJobData(APP_ID_KEY, appId).build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(appId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String appId = null;
    try {
      appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
      Application application = wingsPersistence.get(Application.class, appId);
      if (application == null) {
        jobScheduler.deleteJob(appId, GROUP);
        return;
      }
      final String appIdFinal = appId;
      // The app level lock was a work around for the threading issue we observed in quartz scheduler. The execute() was
      // getting called on all the managers. Its supposed to call it only on one manager. This is a way to stop that
      // from happening.
      try (AcquiredLock lock = persistentLocker.tryToAcquireLock(Application.class, appId, Duration.ofSeconds(1))) {
        if (lock == null) {
          return;
        }
        executorService.submit(() -> executeInternal(appIdFinal));
      }
    } catch (WingsException exception) {
      // do nothing. Only one manager should acquire the lock.
    } catch (Exception ex) {
      logger.warn("Error while looking up appId instances for app: {}", appId, ex);
    }
  }

  private void executeInternal(String appId) {
    try {
      final String appIdFinal = appId;
      logger.info("Executing instance sync job for appId:" + appId);
      PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
      pageRequest.addFilter("appId", Operator.EQ, appId);
      PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
      // Response only contains id
      List<InfrastructureMapping> infraMappingList = response.getResponse();

      infraMappingList.forEach(infraMapping -> {
        String infraMappingId = infraMapping.getUuid();
        InfrastructureMappingType infraMappingType =
            Util.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());
        try (AcquiredLock lock = persistentLocker.tryToAcquireLock(
                 InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(180))) {
          if (lock == null) {
            logger.warn("Couldn't acquire infra lock for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
            return;
          }

          try {
            InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infraMappingType);
            if (instanceHandler == null) {
              logger.warn("Instance handler null for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
              return;
            }
            logger.info("Instance sync job started for infraMapping [{}]", infraMappingId);
            instanceHandler.syncInstances(appIdFinal, infraMappingId);
            logger.info("Instance sync job completed for infraMapping [{}]", infraMappingId);
          } catch (Exception ex) {
            logger.warn("Instance sync job failed for infraMappingId [{}]", infraMappingId, ex);
          }
        } catch (Exception e) {
          logger.warn("Failed to acquire lock for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
        }
      });

      logger.info("Instance sync job done for appId:" + appId);
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception ex) {
      logger.warn("Error while syncing instances for app: {}", appId, ex);
    }
  }
}

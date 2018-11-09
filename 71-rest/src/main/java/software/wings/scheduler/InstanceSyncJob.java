package software.wings.scheduler;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.scheduler.PersistentScheduler;
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
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.InfrastructureMappingService;

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

  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private InstanceHelper instanceHelper;
  @Inject private WingsPersistence wingsPersistence;

  @Inject @Named("ServiceJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;

  public static void add(PersistentScheduler jobScheduler, String appId) {
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
      executorService.submit(() -> executeInternal(appIdFinal));
    } catch (WingsException exception) {
      // do nothing. Only one manager should acquire the lock.
    } catch (Exception ex) {
      logger.warn(format("Error while looking up appId instances for app: %s", appId), ex);
    }
  }

  private void executeInternal(String appId) {
    try {
      // The app level lock was a work around for the threading issue we observed in quartz scheduler. The execute() was
      // getting called on all the managers. Its supposed to call it only on one manager. This is a way to stop that
      // from happening.
      // The lock timeout duration is given a high number intentionally since
      // it handles sync for all the infra mappings and all instances and each sync operation is a blocking delegate
      // task.
      try (AcquiredLock lock = persistentLocker.tryToAcquireLock(Application.class, appId, Duration.ofMinutes(10))) {
        if (lock == null) {
          return;
        }

        logger.info("Executing instance sync job for appId:" + appId);

        PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
        pageRequest.addFilter("appId", Operator.EQ, appId);
        pageRequest.addOrder("envId", OrderType.ASC);
        PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
        // Response only contains id
        List<InfrastructureMapping> infraMappingList = response.getResponse();
        infraMappingList.forEach(infrastructureMapping -> { instanceHelper.syncNow(appId, infrastructureMapping); });

        logger.info("Instance sync done for appId:" + appId);
      }
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception ex) {
      logger.warn(format("Error while syncing instances for app: %s", appId), ex);
    }
  }
}

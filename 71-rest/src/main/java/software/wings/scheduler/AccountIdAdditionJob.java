package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.UuidAccess.ID_KEY;

import com.google.inject.Inject;

import io.harness.lock.AcquiredLock;
import io.harness.persistence.HIterator;
import io.harness.scheduler.BackgroundExecutorService;
import io.harness.scheduler.BackgroundSchedulerLocker;
import io.harness.scheduler.PersistentScheduler;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import java.time.Duration;

/**
 * @author rktummala on 03/05/19
 */
@DisallowConcurrentExecution
public class AccountIdAdditionJob implements Job {
  private static final String CRON_NAME = "ACCOUNT_ID_ADDITION_CRON_NAME";
  private static final String CRON_GROUP = "ACCOUNT_ID_ADDITION_CRON_GROUP";
  private static final String LOCK = "ACCOUNT_ID_ADDITION";
  private static final Logger logger = LoggerFactory.getLogger(AccountIdAdditionJob.class);

  @Inject private BackgroundExecutorService executorService;
  @Inject private BackgroundSchedulerLocker persistentLocker;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running account id addition job asynchronously");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    try (AcquiredLock lock = persistentLocker.getLocker().tryToAcquireLock(LOCK, Duration.ofMinutes(5))) {
      if (lock == null) {
        logger.info("Lock not acquired");
        return;
      }

      try {
        logger.info("Account id addition job start");
        setAccountIdForEntities(Service.class);
        setAccountIdForEntities(Environment.class);
        setAccountIdForEntities(InfrastructureProvisioner.class);
        setAccountIdForEntities(Workflow.class);
        setAccountIdForEntities(Pipeline.class);
        logger.info("Account id addition job end");
      } catch (Exception e) {
        logger.warn("Account id addition job failed to execute", e);
      }
    }
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(AccountIdAdditionJob.class)
                        .withIdentity(CRON_NAME, CRON_GROUP)
                        .withDescription("Account Id Addition Job")
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(CRON_NAME, CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInMinutes(5)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  private <T extends Base> void setAccountIdForEntities(Class<T> clazz) {
    try (HIterator<T> entities = new HIterator<>(wingsPersistence.createQuery(clazz)
                                                     .field("accountId")
                                                     .doesNotExist()
                                                     .project(ID_KEY, true)
                                                     .project("appId", true)
                                                     .fetch())) {
      while (entities.hasNext()) {
        final T entity = entities.next();
        String accountId = appService.getAccountIdByAppId(entity.getAppId());
        if (isEmpty(accountId)) {
          continue;
        }
        wingsPersistence.updateField(clazz, entity.getUuid(), "accountId", accountId);
      }
    }
  }
}

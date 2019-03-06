package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.UuidAccess.ID_KEY;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.persistence.HIterator;
import io.harness.scheduler.BackgroundExecutorService;
import io.harness.scheduler.BackgroundSchedulerLocker;
import io.harness.scheduler.PersistentScheduler;
import org.mongodb.morphia.query.Query;
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
    Query<T> query = wingsPersistence.createQuery(clazz);
    query.or(query.criteria("accountId").doesNotExist(), query.criteria("accountId").equal(null));
    query.project(ID_KEY, true);
    query.project("appId", true);

    try (HIterator<T> entities = new HIterator<>(query.fetch())) {
      while (entities.hasNext()) {
        try {
          final T entity = entities.next();
          String appId = entity.getAppId();
          String entityId = entity.getUuid();
          String accountId = appService.getAccountIdByAppId(appId);

          if (isEmpty(accountId)) {
            continue;
          }
          wingsPersistence.updateField(clazz, entityId, "accountId", accountId);
        } catch (WingsException ex) {
          // There are some orphan entities, whose appId is invalid
          if (!ErrorCode.INVALID_ARGUMENT.equals(ex.getCode())) {
            logger.warn("Account id addition job failed to execute for entity {}", clazz.getSimpleName(), ex);
          }
        } catch (Exception ex) {
          logger.warn("Account id addition job failed to execute for entity {}", clazz.getSimpleName(), ex);
        }
      }
    }
  }
}

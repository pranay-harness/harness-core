package software.wings.scheduler;

import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;

public class PersistentLockCleanupJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(PersistentLockCleanupJob.class);

  public static final String NAME = "MAINTENANCE";
  public static final String GROUP = "PERSISTENT_LOCK_CRON_GROUP";

  private static final int POLL_INTERVAL = 60 * 60;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;

  public static void add(QuartzScheduler jobScheduler) {
    jobScheduler.deleteJob(NAME, GROUP);

    JobDetail job = JobBuilder.newJob(PersistentLockCleanupJob.class).withIdentity(NAME, GROUP).build();

    OffsetDateTime startTime = OffsetDateTime.now().plusMinutes(10);

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(NAME, GROUP)
            .startAt(Date.from(startTime.toInstant()))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  public DBCursor queryOldLocks(OffsetDateTime date) {
    final BasicDBObject filter = new BasicDBObject()
                                     .append("lockState", "unlocked")
                                     .append("lastUpdated", new BasicDBObject("$lt", Date.from(date.toInstant())));

    return wingsPersistence.getCollection("locks").find(filter).limit(1000);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running PersistentLockCleanup Job asynchronously and returning");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    try (AcquiredLock lock = persistentLocker.acquireLock(PersistentLocker.class, NAME, Duration.ofMinutes(1))) {
      OffsetDateTime startTime = OffsetDateTime.now().minusWeeks(1);
      final DBCursor locks = queryOldLocks(startTime);

      while (locks.hasNext()) {
        delete(locks.next().get("_id"));
      }
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("Error seen in the PersistentLockCleanupJob execute call", exception);
    }
  }

  private void delete(Object lock) {
    // Do not delete the lock willy-nilly. We are in race between the query for unlocked state, the deleting
    // and some other process attempting to lock the same locks.
    //
    // The lock needs to be deleted only if successfully acquired

    try (AcquiredLock lk = persistentLocker.acquireLock(lock.toString(), Duration.ofSeconds(10))) {
      if (logger.isInfoEnabled()) {
        logger.info("Destroy outdated lock " + lock.toString());
      }
      persistentLocker.destroy(lk);
    } catch (WingsException exception) {
      // Nothing to do. If we did not get the lock or we succeeded to destroy it - either way move to the
      // next one.
    }
  }
}

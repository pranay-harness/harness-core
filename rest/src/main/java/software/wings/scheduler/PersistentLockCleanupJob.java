package software.wings.scheduler;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
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
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;

import java.time.Duration;
import java.util.Calendar;

/**
 * Created by rishi on 4/6/17.
 */
public class PersistentLockCleanupJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(PersistentLockCleanupJob.class);

  public static final String NAME = "MAINTENANCE";
  public static final String GROUP = "PERSISTENT_LOCK_CRON_GROUP";
  private static final int POLL_INTERVAL = 60 * 60;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;

  public static void add(QuartzScheduler jobScheduler) {
    jobScheduler.deleteJob(NAME, GROUP);

    JobDetail job = JobBuilder.newJob(PersistentLockCleanupJob.class).withIdentity(NAME, GROUP).build();

    Calendar startTime = Calendar.getInstance();
    startTime.add(Calendar.MINUTE, 10);

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(NAME, GROUP)
            .startAt(startTime.getTime())
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  public DBCursor queryOldLocks(Calendar date) {
    final BasicDBObject filter = new BasicDBObject()
                                     .append("lockState", "unlocked")
                                     .append("lastUpdated", new BasicDBObject("$lt", date.getTime()));

    return wingsPersistence.getCollection("locks").find(filter).limit(1000);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try (AcquiredLock lock = persistentLocker.acquireLock(PersistentLocker.class, NAME, Duration.ofMinutes(1))) {
      Calendar date = Calendar.getInstance();
      date.add(Calendar.HOUR, -7 * 24);

      final DBCursor locks = queryOldLocks(date);

      while (locks.hasNext()) {
        final Object object = locks.next().get("_id");

        // Do not delete the lock willy-nilly. We are in race between the query for unlocked state, the deleting
        // and some other process attempting to lock the same locks.
        //
        // The lock needs to be deleted only if successfully acquired

        try (AcquiredLock lk = persistentLocker.acquireLock(object.toString(), Duration.ofSeconds(10))) {
          logger.info("Destroy outdated lock " + object.toString());
          persistentLocker.destroy(lk);
        } catch (WingsException exception) {
          // Nothing to do. If we did not get the lock or we succeeded to destroy it - either way move to the
          // next one.
        }
      }
    } catch (WingsException exception) {
      exception.logProcessedMessages();
    } catch (Exception exception) {
      logger.error("Error seen in the PersistentLockCleanupJob execute call", exception);
    }
  }
}

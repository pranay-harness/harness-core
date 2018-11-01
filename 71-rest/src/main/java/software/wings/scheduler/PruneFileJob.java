package software.wings.scheduler;

import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppContainer;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

public class PruneFileJob implements Job {
  private static Logger logger = LoggerFactory.getLogger(PruneFileJob.class);

  public static final String GROUP = "PRUNE_FILE_GROUP";

  public static final String ENTITY_CLASS_KEY = "class";
  public static final String ENTITY_ID_KEY = "entityId";
  public static final String BUCKET_KEY = "bucket";

  @Inject private WingsPersistence wingsPersistence;

  @Inject private FileService fileService;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public static Trigger defaultTrigger(String id, Duration delay) {
    final TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger().withIdentity(id, GROUP).withSchedule(
        SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).withRepeatCount(24));
    OffsetDateTime time = OffsetDateTime.now().plus(delay);
    builder.startAt(Date.from(time.toInstant()));
    return builder.build();
  }

  public static void addDefaultJob(
      PersistentScheduler jobScheduler, Class cls, String entityId, FileBucket fileBucket) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(entityId, GROUP);

    JobDetail details = JobBuilder.newJob(PruneFileJob.class)
                            .withIdentity(entityId, GROUP)
                            .usingJobData(ENTITY_CLASS_KEY, cls.getCanonicalName())
                            .usingJobData(ENTITY_ID_KEY, entityId)
                            .usingJobData(BUCKET_KEY, fileBucket.name())
                            .build();

    Trigger trigger = defaultTrigger(entityId, ofSeconds(5));

    jobScheduler.scheduleJob(details, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
    String className = map.getString(ENTITY_CLASS_KEY);
    String entityId = map.getString(ENTITY_ID_KEY);

    try {
      Class cls = Class.forName(className);

      if (!className.equals(AppContainer.class.getCanonicalName())
          && !className.equals(Artifact.class.getCanonicalName())) {
        logger.error("Unsupported class [{}] was scheduled for pruning.", className);
      } else if (wingsPersistence.get(cls, entityId) != null) {
        // If this is the first try the job might of being started way to soon before the entity was
        // deleted. We would like to give at least one more try to pruning.
        if (jobExecutionContext.getPreviousFireTime() == null) {
          return;
        }
        logger.warn("This warning should be happening very rarely. If you see this often, please investigate.\n"
            + "The only case this warning should show is if there was a crash or network disconnect in the race of "
            + "the prune job schedule and the parent entity deletion.");
      } else {
        String bucket = map.getString(BUCKET_KEY);
        fileService.deleteAllFilesForEntity(entityId, FileBucket.valueOf(bucket));
      }
    } catch (ClassNotFoundException e) {
      logger.error("The class this job is for no longer exists!!!", e);
    } catch (Exception e) {
      logger.error("PruneFileJob will have to retry to delete the files", e);
      return;
    }

    jobScheduler.deleteJob(entityId, GROUP);
  }
}

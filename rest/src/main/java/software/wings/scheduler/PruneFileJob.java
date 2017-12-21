package software.wings.scheduler;

import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppContainer;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

public class PruneFileJob implements Job {
  protected static Logger logger = LoggerFactory.getLogger(PruneFileJob.class);

  public static final String GROUP = "PRUNE_FILE_GROUP";

  public static final String OBJECT_CLASS_KEY = "class";
  public static final String OBJECT_ID_KEY = "object_id";
  public static final String BUCKET_KEY = "bucket";
  public static final String UUIDS_KEY = "uuids";

  @Inject private WingsPersistence wingsPersistence;

  @Inject private FileService fileService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static void addDefaultJob(
      QuartzScheduler jobScheduler, Class cls, String objectId, FileBucket fileBucket, List<String> uuids) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(objectId, PruneFileJob.GROUP);

    JobDetail details = JobBuilder.newJob(PruneFileJob.class)
                            .withIdentity(objectId, PruneFileJob.GROUP)
                            .usingJobData(PruneFileJob.OBJECT_CLASS_KEY, cls.getCanonicalName())
                            .usingJobData(PruneFileJob.OBJECT_ID_KEY, objectId)
                            .usingJobData(PruneFileJob.BUCKET_KEY, fileBucket.name())
                            .usingJobData(PruneFileJob.UUIDS_KEY, String.join(",", uuids))
                            .build();

    Trigger trigger = PruneObjectJob.defaultTrigger(objectId);

    jobScheduler.scheduleJob(details, trigger);
  }

  public interface PruneService<T> { public void prune(T descending); }

  private boolean pruneFiles(String bucket, String uuids) {
    try {
      String[] fileUuids = uuids.split(",");
      FileBucket fileBucket = FileBucket.valueOf(bucket);
      Arrays.stream(fileUuids).forEach(uuid -> fileService.deleteFile(uuid, fileBucket));
    } catch (Exception e) {
      logger.error(e.toString());
      return false;
    }
    return true;
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
    String className = map.getString(OBJECT_CLASS_KEY);
    String objectId = map.getString(OBJECT_ID_KEY);

    try {
      Class cls = Class.forName(className);

      if (!className.equals(AppContainer.class.getCanonicalName())
          && !className.equals(Artifact.class.getCanonicalName())) {
        logger.error("Unsupported class [{}] was scheduled for pruning.", className);
      } else if (wingsPersistence.get(cls, objectId) != null) {
        // If this is the first try the job might of being started way to soon before the object was
        // deleted. We would like to give at least one more try to pruning.
        if (jobExecutionContext.getPreviousFireTime() == null) {
          return;
        }
        logger.warn("This warning should be happening very rarely. If you see this often, please investigate.\n"
            + "The only case this warning should show is if there was a crash or network disconnect in the race of "
            + "the prune job schedule and the parent object deletion.");
      } else {
        String bucket = map.getString(BUCKET_KEY);
        String uuids = map.getString(UUIDS_KEY);

        if (!pruneFiles(bucket, uuids)) {
          return;
        }
      }
    } catch (ClassNotFoundException e) {
      logger.error(e.toString());
    }

    jobScheduler.deleteJob(objectId, GROUP);
  }
}

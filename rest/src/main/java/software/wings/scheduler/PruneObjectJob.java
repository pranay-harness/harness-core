package software.wings.scheduler;

import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import java.util.Calendar;
import javax.inject.Inject;

public class PruneObjectJob implements Job {
  private static Logger logger = LoggerFactory.getLogger(PruneObjectJob.class);

  public static final String GROUP = "PRUNE_OBJECT_GROUP";

  public static final String OBJECT_CLASS_KEY = "class";
  public static final String OBJECT_ID_KEY = "id";

  @Inject private WingsPersistence wingsPersistence;

  @Inject private AppService appService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static Trigger defaultTrigger(String id) {
    // Run the job in about 5 seconds, we do not want to run
    // it without given enough time to the object to be deleted.
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, 5);
    return TriggerBuilder.newTrigger()
        .withIdentity(id, GROUP)
        .startAt(calendar.getTime())
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).withRepeatCount(24))
        .build();
  }

  private boolean prune(String className, String id) {
    try {
      if (className.equals(Application.class.getCanonicalName())) {
        appService.pruneDescendingObjects(id);
      } else {
        logger.error("Unsupported class [{}] was scheduled for pruning.", className);
      }
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

    String id = map.getString(OBJECT_ID_KEY);
    try {
      Class cls = Class.forName(className);

      if (wingsPersistence.get(cls, id) != null) {
        // If this is the first try the job might of being started way to soon before the object was
        // deleted. We would like to give at least one more try to pruning.
        if (jobExecutionContext.getPreviousFireTime() == null) {
          return;
        }
        logger.warn("This warning should be happening very rarely. If you see this often, please investigate.\n"
            + "The only case this warning should show is if there was a crash or network disconnect in the race of "
            + "the prune job schedule and the parent object deletion.");

      } else if (!prune(className, id)) {
        return;
      }
    } catch (ClassNotFoundException e) {
      logger.error(e.toString());
    }

    jobScheduler.deleteJob(id, GROUP);
  }
}

package software.wings.scheduler;

import static software.wings.beans.ErrorCode.UNKNOWN_ERROR;
import static software.wings.beans.ResponseMessage.Builder.aResponseMessage;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.ResponseMessage;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PruneObjectJob implements Job {
  protected static Logger logger = LoggerFactory.getLogger(PruneObjectJob.class);

  public static final String GROUP = "PRUNE_OBJECT_GROUP";

  public static final String OBJECT_CLASS_KEY = "class";
  public static final String APP_ID_KEY = "app_id";
  public static final String OBJECT_ID_KEY = "object_id";

  @Inject private WingsPersistence wingsPersistence;

  @Inject private ActivityService activityService;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;

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

  public static void addDefaultJob(QuartzScheduler jobScheduler, Class cls, String appId, String objectId) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(objectId, GROUP);

    JobDetail details = JobBuilder.newJob(PruneObjectJob.class)
                            .withIdentity(objectId, PruneObjectJob.GROUP)
                            .usingJobData(PruneObjectJob.OBJECT_CLASS_KEY, cls.getCanonicalName())
                            .usingJobData(PruneObjectJob.APP_ID_KEY, appId)
                            .usingJobData(PruneObjectJob.OBJECT_ID_KEY, objectId)
                            .build();

    org.quartz.Trigger trigger = PruneObjectJob.defaultTrigger(objectId);

    jobScheduler.scheduleJob(details, trigger);
  }

  public interface PruneService<T> { public void prune(T descending); }

  public static <T> void pruneDescendingObjects(
      List<T> descendingServices, String appId, String objectId, PruneService<T> lambda) {
    List<ResponseMessage> messages = new ArrayList<>();

    for (T descending : descendingServices) {
      try {
        lambda.prune(descending);
      } catch (WingsException e) {
        messages.addAll(e.getResponseMessageList());
      } catch (RuntimeException e) {
        messages.add(aResponseMessage().withCode(UNKNOWN_ERROR).withMessage(e.getMessage()).build());
      }
    }

    if (!messages.isEmpty()) {
      throw new WingsException(
          messages, "Fail to prune some of the objects for app: " + appId + ", object: " + objectId, (Throwable) null);
    }
  }

  private boolean prune(String className, String appId, String objectId) {
    if (className.equals(Application.class.getCanonicalName())) {
      if (!appId.equals(objectId)) {
        throw new WingsException("Prune job is incorrectly initialized with objectId: " + objectId
            + " and appId: " + appId + " being different for the application class");
      }
    }

    try {
      if (className.equals(Activity.class.getCanonicalName())) {
        activityService.pruneDescendingObjects(appId, objectId);
      } else if (className.equals(Application.class.getCanonicalName())) {
        appService.pruneDescendingObjects(appId);
      } else if (className.equals(Environment.class.getCanonicalName())) {
        environmentService.pruneDescendingObjects(appId, objectId);
      } else if (className.equals(Pipeline.class.getCanonicalName())) {
        pipelineService.pruneDescendingObjects(appId, objectId);
      } else if (className.equals(Service.class.getCanonicalName())) {
        serviceResourceService.pruneDescendingObjects(appId, objectId);
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

    String appId = map.getString(APP_ID_KEY);
    String objectId = map.getString(OBJECT_ID_KEY);
    try {
      Class cls = Class.forName(className);

      if (wingsPersistence.get(cls, objectId) != null) {
        // If this is the first try the job might of being started way to soon before the object was
        // deleted. We would like to give at least one more try to pruning.
        if (jobExecutionContext.getPreviousFireTime() == null) {
          return;
        }
        logger.warn("This warning should be happening very rarely. If you see this often, please investigate.\n"
            + "The only case this warning should show is if there was a crash or network disconnect in the race of "
            + "the prune job schedule and the parent object deletion.");

      } else if (!prune(className, appId, objectId)) {
        return;
      }
    } catch (ClassNotFoundException e) {
      logger.error(e.toString());
    }

    jobScheduler.deleteJob(objectId, GROUP);
  }
}

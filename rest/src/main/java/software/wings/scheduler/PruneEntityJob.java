package software.wings.scheduler;

import static software.wings.beans.ErrorCode.UNKNOWN_ERROR;

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
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.ResponseMessage;
import software.wings.beans.Service;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PruneEntityJob implements Job {
  protected static Logger logger = LoggerFactory.getLogger(PruneEntityJob.class);

  public static final String GROUP = "PRUNE_ENTITY_GROUP";

  public static final String ENTITY_CLASS_KEY = "class";
  public static final String ENTITY_ID = "entityId";
  public static final String APP_ID_KEY = "appId";

  @Inject private WingsPersistence wingsPersistence;

  @Inject private ActivityService activityService;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private HostService hostService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static Trigger defaultTrigger(String id) {
    // Run the job in about 5 seconds, we do not want to run
    // it without given enough time to the entity to be deleted.
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, 5);
    return TriggerBuilder.newTrigger()
        .withIdentity(id, GROUP)
        .startAt(calendar.getTime())
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).withRepeatCount(24))
        .build();
  }

  public static void addDefaultJob(QuartzScheduler jobScheduler, Class cls, String appId, String entityId) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(entityId, GROUP);

    JobDetail details = JobBuilder.newJob(PruneEntityJob.class)
                            .withIdentity(entityId, PruneEntityJob.GROUP)
                            .usingJobData(PruneEntityJob.ENTITY_CLASS_KEY, cls.getCanonicalName())
                            .usingJobData(PruneEntityJob.APP_ID_KEY, appId)
                            .usingJobData(PruneEntityJob.ENTITY_ID, entityId)
                            .build();

    org.quartz.Trigger trigger = PruneEntityJob.defaultTrigger(entityId);

    jobScheduler.scheduleJob(details, trigger);
  }

  public interface PruneService<T> { public void prune(T descending); }

  public static <T> void pruneDescendingEntities(
      List<T> descendingServices, String appId, String entiryId, PruneService<T> lambda) {
    List<ResponseMessage> messages = new ArrayList<>();

    for (T descending : descendingServices) {
      try {
        lambda.prune(descending);
      } catch (WingsException e) {
        messages.addAll(e.getResponseMessageList());
      } catch (RuntimeException e) {
        messages.add(ResponseMessage.builder().code(UNKNOWN_ERROR).message(e.getMessage()).build());
      }
    }

    if (!messages.isEmpty()) {
      throw new WingsException(
          messages, "Fail to prune some of the entitys for app: " + appId + ", entity: " + entiryId, (Throwable) null);
    }
  }

  private boolean prune(String className, String appId, String entityId) {
    if (className.equals(Application.class.getCanonicalName())) {
      if (!appId.equals(entityId)) {
        throw new WingsException("Prune job is incorrectly initialized with entityId: " + entityId
            + " and appId: " + appId + " being different for the application class");
      }
    }

    try {
      if (className.equals(Activity.class.getCanonicalName())) {
        activityService.pruneDescendingEntities(appId, entityId);
      } else if (className.equals(Application.class.getCanonicalName())) {
        appService.pruneDescendingEntities(appId);
      } else if (className.equals(Environment.class.getCanonicalName())) {
        environmentService.pruneDescendingEntities(appId, entityId);
      } else if (className.equals(Host.class.getCanonicalName())) {
        hostService.pruneDescendingEntities(appId, entityId);
      } else if (className.equals(InfrastructureMapping.class.getCanonicalName())) {
        infrastructureMappingService.pruneDescendingEntities(appId, entityId);
      } else if (className.equals(Pipeline.class.getCanonicalName())) {
        pipelineService.pruneDescendingEntities(appId, entityId);
      } else if (className.equals(Service.class.getCanonicalName())) {
        serviceResourceService.pruneDescendingEntities(appId, entityId);
      } else {
        logger.error("Unsupported class [{}] was scheduled for pruning.", className);
      }
    } catch (Exception e) {
      logger.error("", e);
      return false;
    }
    return true;
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
    String className = map.getString(ENTITY_CLASS_KEY);

    String appId = map.getString(APP_ID_KEY);
    String entityId = map.getString(ENTITY_ID);
    try {
      Class cls = Class.forName(className);

      if (wingsPersistence.get(cls, entityId) != null) {
        // If this is the first try the job might of being started way to soon before the entity was
        // deleted. We would like to give at least one more try to pruning.
        if (jobExecutionContext.getPreviousFireTime() == null) {
          return;
        }
        logger.warn("This warning should be happening very rarely. If you see this often, please investigate.\n"
            + "The only case this warning should show is if there was a crash or network disconnect in the race of "
            + "the prune job schedule and the parent entity deletion.");

      } else if (!prune(className, appId, entityId)) {
        return;
      }
    } catch (ClassNotFoundException e) {
      logger.error("The class this job is for no longer exists!!!", e);
    }

    jobScheduler.deleteJob(entityId, GROUP);
  }
}

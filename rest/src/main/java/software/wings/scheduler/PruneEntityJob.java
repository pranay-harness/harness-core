package software.wings.scheduler;

import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.CauseCollection;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.function.Consumer;

public class PruneEntityJob implements Job {
  protected static Logger logger = LoggerFactory.getLogger(PruneEntityJob.class);

  public static final String GROUP = "PRUNE_ENTITY_GROUP";

  public static final String ENTITY_CLASS_KEY = "class";
  public static final String ENTITY_ID_KEY = "entityId";
  public static final String APP_ID_KEY = "appId";

  @Inject private WingsPersistence wingsPersistence;

  @Inject private ActivityService activityService;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private HostService hostService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowService workflowService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static Trigger defaultTrigger(String id, Duration delay) {
    final TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger().withIdentity(id, GROUP).withSchedule(
        SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).withRepeatCount(24));

    if (delay.toMillis() > 5) {
      // Run the job with daley. This can be used to give enough time the entity to be deleted.
      OffsetDateTime time = OffsetDateTime.now().plus(delay);
      builder.startAt(Date.from(time.toInstant()));
    }

    return builder.build();
  }

  public static void addDefaultJob(
      QuartzScheduler jobScheduler, Class cls, String appId, String entityId, Duration delay) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(entityId, GROUP);

    JobDetail details = JobBuilder.newJob(PruneEntityJob.class)
                            .withIdentity(entityId, GROUP)
                            .usingJobData(ENTITY_CLASS_KEY, cls.getCanonicalName())
                            .usingJobData(APP_ID_KEY, appId)
                            .usingJobData(ENTITY_ID_KEY, entityId)
                            .build();

    Trigger trigger = defaultTrigger(entityId, delay);

    jobScheduler.scheduleJob(details, trigger);
  }

  public static <T> void pruneDescendingEntities(Iterable<T> descendingServices, Consumer<T> lambda) {
    CauseCollection causeCollection = new CauseCollection();
    boolean succeeded = true;
    for (T descending : descendingServices) {
      try {
        logger.info("Pruning descending entities for {} ", descending.getClass());
        lambda.accept(descending);
      } catch (WingsException exception) {
        succeeded = false;
        exception.logProcessedMessages(MANAGER, logger);
      } catch (RuntimeException e) {
        succeeded = false;
        causeCollection.addCause(e);
      }
    }
    if (!succeeded) {
      throw new WingsException(causeCollection.getCause());
    }
  }

  private boolean prune(String className, String appId, String entityId) {
    logger.info("Pruning Entity {} {} for appId {}", className, entityId, appId);
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
      } else if (className.equals(ArtifactStream.class.getCanonicalName())) {
        artifactStreamService.pruneDescendingEntities(appId, entityId);
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
      } else if (className.equals(Workflow.class.getCanonicalName())) {
        workflowService.pruneDescendingEntities(appId, entityId);
      } else {
        logger.error("Unsupported class [{}] was scheduled for pruning.", className);
      }
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
      return false;
    } catch (RuntimeException e) {
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
    String entityId = map.getString(ENTITY_ID_KEY);
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

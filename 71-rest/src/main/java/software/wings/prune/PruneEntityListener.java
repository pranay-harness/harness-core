package software.wings.prune;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.prune.PruneEvent.MAX_RETRIES;

import com.google.inject.Inject;

import io.harness.exception.CauseCollection;
import io.harness.exception.WingsException;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.logging.ExceptionLogger;
import io.harness.manage.GlobalContextManager;
import io.harness.queue.QueueListener;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Slf4j
public class PruneEntityListener extends QueueListener<PruneEvent> {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private ActivityService activityService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private EnvironmentService environmentService;
  @Inject private HostService hostService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowService workflowService;
  @Inject private ExecutorService executorService;

  public PruneEntityListener() {
    super(true);
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
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (RuntimeException e) {
        succeeded = false;
        causeCollection.addCause(e);
      }
    }
    if (!succeeded) {
      throw new WingsException(causeCollection.getCause());
    }
  }

  private boolean prune(Class clz, String appId, String entityId) {
    logger.info("Pruning Entity {} {} for appId {}", clz.getCanonicalName(), entityId, appId);
    if (clz.equals(Application.class)) {
      if (!appId.equals(entityId)) {
        logger.warn("Prune job is incorrectly initialized with entityId: " + entityId + " and appId: " + appId
            + " being different for the application class");
        return true;
      }
    }

    try {
      if (clz.equals(Activity.class)) {
        activityService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(Application.class)) {
        appService.pruneDescendingEntities(appId);
      } else if (clz.equals(ArtifactStream.class)) {
        artifactStreamService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(Environment.class)) {
        environmentService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(Host.class)) {
        hostService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(InfrastructureMapping.class)) {
        infrastructureMappingService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(Pipeline.class)) {
        pipelineService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(Service.class)) {
        serviceResourceService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(Workflow.class)) {
        workflowService.pruneDescendingEntities(appId, entityId);
      } else if (clz.equals(InfrastructureProvisioner.class)) {
        infrastructureProvisionerService.pruneDescendingEntities(appId, entityId);
      } else {
        logger.error("Unsupported class [{}] was scheduled for pruning.", clz.getCanonicalName());
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      return false;
    } catch (RuntimeException e) {
      logger.error("", e);
      return false;
    }
    return true;
  }

  @Override
  public void onMessage(PruneEvent message) {
    try {
      Class clz = Class.forName(message.getEntityClass());

      if (wingsPersistence.get(clz, message.getEntityId()) != null) {
        // if it is the first try, give it at least one more chance
        if (message.getRetries() == MAX_RETRIES) {
          throw new WingsException("The object still exist, lets try later");
        }
        logger.warn("This warning should be happening very rarely. If you see this often, please investigate.\n"
            + "The only case this warning should show is if there was a crash or network disconnect in the race of "
            + "the prune job schedule and the parent entity deletion.");

      } else {
        GlobalContextManager.upsertGlobalContextRecord(PurgeGlobalContextData.builder().build());
        if (!prune(clz, message.getAppId(), message.getEntityId())) {
          throw new WingsException("The prune failed this time");
        }
      }
    } catch (ClassNotFoundException ignore) {
      // ignore events for objects that no longer exists
    }
  }

  @Override
  protected void requeue(PruneEvent message) {
    getQueue().requeue(
        message.getId(), message.getRetries() - 1, Date.from(OffsetDateTime.now().plusHours(1).toInstant()));
  }
}

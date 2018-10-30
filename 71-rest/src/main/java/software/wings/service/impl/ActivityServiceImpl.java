package software.wings.service.impl;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.ADMIN;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Event.Builder.anEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.scheduler.PersistentScheduler;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Activity;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Event.Type;
import software.wings.beans.Log;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.PruneEntityJob;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ownership.OwnedByActivity;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class ActivityServiceImpl implements ActivityService {
  public static final int MAX_ACTIVITY_VERSION_RETRY = 5;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LogService logService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private EventEmitter eventEmitter;
  private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

  @Inject @Named("JobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public PageResponse<Activity> list(PageRequest<Activity> pageRequest) {
    return wingsPersistence.query(Activity.class, pageRequest);
  }

  @Override
  public Activity get(String id, String appId) {
    Activity activity = wingsPersistence.get(Activity.class, appId, id);
    if (activity == null) {
      throw new WingsException(INVALID_ARGUMENT, ADMIN).addParam("args", "Activity doesn't exist");
    }
    return activity;
  }

  @Override
  public Activity save(Activity activity) {
    wingsPersistence.save(activity);
    if (isNotBlank(activity.getServiceInstanceId())) {
      serviceInstanceService.updateActivity(activity);
    }
    eventEmitter.send(Channel.ACTIVITIES,
        anEvent()
            .withType(Type.CREATE)
            .withUuid(activity.getUuid())
            .withAppId(activity.getAppId())
            .withEnvId(activity.getEnvironmentId())
            .build());
    return activity;
  }

  @Override
  public void updateStatus(String activityId, String appId, ExecutionStatus status) {
    wingsPersistence.update(
        wingsPersistence.createQuery(Activity.class).filter(ID_KEY, activityId).filter("appId", appId),
        wingsPersistence.createUpdateOperations(Activity.class).set("status", status));
    Activity activity = get(activityId, appId);
    if (isNotBlank(activity.getServiceInstanceId())) {
      serviceInstanceService.updateActivity(activity);
    }
    eventEmitter.send(Channel.ACTIVITIES,
        anEvent()
            .withType(Type.UPDATE)
            .withUuid(activity.getUuid())
            .withAppId(activity.getAppId())
            .withEnvId(activity.getEnvironmentId())
            .build());
  }

  @Override
  public List<CommandUnitDetails> getCommandUnits(String appId, String activityId) {
    Activity activity = get(activityId, appId);
    List<CommandUnitDetails> rv = new ArrayList<>();
    if (activity.getCommandUnitType() != null) {
      switch (activity.getCommandUnitType()) {
        case COMMAND:
          List<CommandUnit> commandUnits = activity.getCommandUnits();
          for (CommandUnit commandUnit : commandUnits) {
            rv.add(CommandUnitDetails.builder()
                       .commandExecutionStatus(commandUnit.getCommandExecutionStatus())
                       .name(commandUnit.getName())
                       .commandUnitType(activity.getCommandUnitType())
                       .build());
          }
          break;
        case JENKINS:
        case HELM:
        case KUBERNETES_STEADY_STATE_CHECK:
        case ECS_STEADY_STATE_CHECK:
        case AWS_AMI_SWITCH_ROUTES:
        case KUBERNETES_SWAP_SERVICE_SELECTORS:
          rv.add(CommandUnitDetails.builder()
                     .commandExecutionStatus(CommandExecutionStatus.translateExecutionStatus(activity.getStatus()))
                     .name(activity.getCommandUnitType().getName())
                     .commandUnitType(activity.getCommandUnitType())
                     .build());
          break;
        case PCF_SETUP:
        case PCF_RESIZE:
        case PCF_MAP_ROUTE:
        case PCF_BG_SWAP_ROUTE:
          rv.add(CommandUnitDetails.builder()
                     .commandExecutionStatus(CommandExecutionStatus.translateExecutionStatus(activity.getStatus()))
                     .name(activity.getCommandUnitType().getName())
                     .commandUnitType(activity.getCommandUnitType())
                     .build());
          break;
        case KUBERNETES:
          rv.add(CommandUnitDetails.builder()
                     .commandExecutionStatus(CommandExecutionStatus.translateExecutionStatus(activity.getStatus()))
                     .name(activity.getCommandName())
                     .commandUnitType(activity.getCommandUnitType())
                     .build());
          break;
        default:
          throw new IllegalStateException("Invalid command type: " + activity.getCommandUnitType());
      }
    }
    return rv;
  }

  @Override
  public Activity getLastActivityForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(Activity.class).filter("appId", appId).filter("serviceId", serviceId).get();
  }

  @Override
  public Activity getLastProductionActivityForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(Activity.class)
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .filter("environmentType", EnvironmentType.PROD)
        .get();
  }

  @Override
  public boolean delete(String appId, String activityId) {
    PruneEntityJob.addDefaultJob(jobScheduler, Activity.class, appId, activityId, ofSeconds(5), ofSeconds(15));

    return wingsPersistence.delete(
        wingsPersistence.createQuery(Activity.class).filter(Activity.APP_ID_KEY, appId).filter(ID_KEY, activityId));
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String activityId) {
    List<OwnedByActivity> services =
        ServiceClassLocator.descendingServices(this, ActivityServiceImpl.class, OwnedByActivity.class);
    PruneEntityJob.pruneDescendingEntities(services, descending -> descending.pruneByActivity(appId, activityId));
  }

  @Override
  public void updateCommandUnitStatus(Map<String, Map<String, Log>> activityCommandUnitLastLogMap) {
    activityCommandUnitLastLogMap.forEach(this ::updateActivityCommandUnitStatus);
  }

  @Override
  public void updateCommandUnitStatus(
      String appId, String activityId, String unitName, CommandExecutionStatus commandUnitStatus) {
    //    Query<Activity> query =
    //        wingsPersistence.createQuery(Activity.class).filter(Mapper.ID_KEY, activityId).filter("appId",
    //        appId).disableValidation().filter("commandUnits.name",
    //            unitName);
    Query<Activity> query = wingsPersistence.createQuery(Activity.class)
                                .filter(Mapper.ID_KEY, activityId)
                                .filter("appId", appId)
                                .field("commandUnits")
                                .elemMatch(wingsPersistence.createQuery(Command.class).filter("name", unitName));

    UpdateOperations<Activity> updateOperations = wingsPersistence.createUpdateOperations(Activity.class)
                                                      .disableValidation()
                                                      .set("commandUnits.$.commandExecutionStatus", commandUnitStatus);
    wingsPersistence.update(query, updateOperations);
  }

  private void updateActivityCommandUnitStatus(String activityId, Map<String, Log> commandUnitLastLogMap) {
    String appId = commandUnitLastLogMap.values().iterator().next().getAppId();

    commandUnitLastLogMap.forEach((unitName, log) -> {
      Query<Activity> query = wingsPersistence.createQuery(Activity.class)
                                  .filter(Mapper.ID_KEY, activityId)
                                  .filter("appId", appId)
                                  .disableValidation()
                                  .filter("commandUnits.name", unitName);

      UpdateOperations<Activity> updateOperations =
          wingsPersistence.createUpdateOperations(Activity.class)
              .disableValidation()
              .set("commandUnits.$.commandExecutionStatus", log.getCommandExecutionStatus());
      wingsPersistence.update(query, updateOperations);
    });
  }
}

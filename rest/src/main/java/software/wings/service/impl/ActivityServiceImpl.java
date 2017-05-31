package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.QUEUED;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Activity;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Event.Type;
import software.wings.beans.Log;
import software.wings.beans.command.CommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.sm.ExecutionStatus;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
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
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public PageResponse<Activity> list(PageRequest<Activity> pageRequest) {
    return wingsPersistence.query(Activity.class, pageRequest);
  }

  @Override
  public Activity get(String id, String appId) {
    Activity activity = wingsPersistence.get(Activity.class, appId, id);
    if (activity == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Activity doesn't exist");
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
        wingsPersistence.createQuery(Activity.class).field(ID_KEY).equal(activityId).field("appId").equal(appId),
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
  public List<CommandUnit> getCommandUnits(String appId, String activityId) {
    Activity activity = get(activityId, appId);
    return activity.getCommandUnits();
  }

  @Override
  public Activity getLastActivityForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(Activity.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .get();
  }

  @Override
  public Activity getLastProductionActivityForService(String appId, String serviceId) {
    return wingsPersistence.createQuery(Activity.class)
        .field("appId")
        .equal(appId)
        .field("serviceId")
        .equal(serviceId)
        .field("environmentType")
        .equal(EnvironmentType.PROD)
        .get();
  }

  @Override
  public boolean delete(String appId, String activityId) {
    boolean deleted = wingsPersistence.delete(
        wingsPersistence.createQuery(Activity.class).field("appId").equal(appId).field(ID_KEY).equal(activityId));
    if (deleted) {
      logService.deleteActivityLogs(appId, activityId);
    }
    return deleted;
  }

  @Override
  public void deleteByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Activity.class)
        .field("appId")
        .equal(appId)
        .field("environmentId")
        .equal(envId)
        .asKeyList()
        .forEach(activityKey -> delete(appId, (String) activityKey.getId()));
  }

  @Override
  public void updateCommandUnitStatus(Map<String, Map<String, Log>> activityCommandUnitLastLogMap) {
    activityCommandUnitLastLogMap.forEach(this ::updateActivityCommandUnitStatusWithRetry);
  }

  private void updateActivityCommandUnitStatusWithRetry(String activityId, Map<String, Log> commandUnitLastLogMap) {
    boolean isSaved = false;
    int retry = 0;

    do {
      try {
        updateActivityCommandUnitStatus(activityId, commandUnitLastLogMap);
        isSaved = true;
      } catch (ConcurrentModificationException ignored) {
        retry++;
      }
    } while (!isSaved && retry < MAX_ACTIVITY_VERSION_RETRY);

    if (!isSaved) {
      logger.error("Activity:{} commandUnit status couldn't be updated after {} retries", activityId, retry);
    } else if (retry > 0) {
      logger.warn("Version conflict encountered. Resolved in {} retry", retry);
    }
  }

  private void updateActivityCommandUnitStatus(String activityId, Map<String, Log> commandUnitLastLogMap) {
    String appId = commandUnitLastLogMap.values().iterator().next().getAppId();
    Activity activity = get(activityId, appId);
    activity.getCommandUnits().forEach(commandUnit -> {
      Log log = commandUnitLastLogMap.get(commandUnit.getName());
      if (isCommandUnitStatusUpdatableByLogStatus(commandUnit, log)) {
        commandUnit.setCommandExecutionStatus(
            log.getCommandExecutionStatus() != null ? log.getCommandExecutionStatus() : RUNNING);
      }
    });

    activity.getCommandUnits().forEach(commandUnit -> {
      logger.info("Test::: {} - {}" + commandUnit.getName(), commandUnit.getCommandExecutionStatus());
    });
    wingsPersistence.update(activity,
        wingsPersistence.createUpdateOperations(Activity.class).set("commandUnits", activity.getCommandUnits()));
  }

  private boolean isCommandUnitStatusUpdatableByLogStatus(CommandUnit commandUnit, Log log) {
    return (log != null
        && (QUEUED.equals(commandUnit.getCommandExecutionStatus())
               || RUNNING.equals(commandUnit.getCommandExecutionStatus())));
  }
}

package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.CommandUnitType.COMMAND;
import static software.wings.beans.ErrorCodes.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import software.wings.beans.Activity;
import software.wings.beans.Command;
import software.wings.beans.CommandUnit;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class ActivityServiceImpl implements ActivityService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private LogService logService;

  @Override
  public PageResponse<Activity> list(String appId, String envId, PageRequest<Activity> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("environmentId", envId, EQ);

    return wingsPersistence.query(Activity.class, pageRequest);
  }

  @Override
  public Activity get(String id, String appId) {
    return wingsPersistence.get(Activity.class, appId, id);
  }

  @Override
  public Activity save(Activity activity) {
    wingsPersistence.save(activity);
    return activity;
  }

  @Override
  public void updateStatus(String activityId, String appId, Activity.Status activityStatus) {
    wingsPersistence.update(
        wingsPersistence.createQuery(Activity.class).field(ID_KEY).equal(activityId).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Activity.class).set("status", activityStatus));
  }

  @Override
  public List<CommandUnit> getCommandUnits(String appId, String activityId) {
    Activity activity = get(activityId, appId);
    Command command =
        serviceResourceService.getCommandByName(appId, activity.getServiceId(), activity.getCommandName());
    List<CommandUnit> commandUnits = getFlattenCommandUnitList(appId, activity.getServiceId(), command);
    commandUnits.forEach(commandUnit -> {
      commandUnit.setExecutionResult(logService.getUnitExecutionResult(appId, activityId, commandUnit.getName()));
    });
    return commandUnits;
  }

  List<CommandUnit> getFlattenCommandUnitList(String appId, String serviceId, Command command) {
    Command executableCommand = command;
    if (command.getReferenceId() != null) {
      executableCommand = serviceResourceService.getCommandByName(appId, serviceId, command.getReferenceId());
      if (executableCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
    }

    List<CommandUnit> commandUnits = new ArrayList<>();

    executableCommand.getCommandUnits().forEach(commandUnit -> {
      if (COMMAND.equals(commandUnit.getCommandUnitType())) {
        commandUnits.addAll(getFlattenCommandUnitList(appId, serviceId, (Command) commandUnit));
      } else {
        commandUnits.add(commandUnit);
      }
    });
    return commandUnits;
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
}

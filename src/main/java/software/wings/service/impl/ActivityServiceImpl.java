package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.COMMAND_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;

import software.wings.beans.Activity;
import software.wings.beans.Artifact;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.command.CleanupCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.InitCommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Singleton
@ValidateOnExecution
public class ActivityServiceImpl implements ActivityService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private LogService logService;
  @Inject private ArtifactService artifactService;

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
    return activity;
  }

  @Override
  public void updateStatus(String activityId, String appId, ExecutionStatus status) {
    wingsPersistence.update(
        wingsPersistence.createQuery(Activity.class).field(ID_KEY).equal(activityId).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Activity.class).set("status", status));
  }

  @Override
  public List<CommandUnit> getCommandUnits(String appId, String activityId) {
    Activity activity = get(activityId, appId);
    Command command =
        serviceResourceService.getCommandByName(appId, activity.getServiceId(), activity.getCommandName());
    List<CommandUnit> commandUnits = getFlattenCommandUnitList(appId, activity.getServiceId(), command);
    if (commandUnits != null && commandUnits.size() > 0) {
      commandUnits.add(0, new InitCommandUnit());
      commandUnits.add(new CleanupCommandUnit());
      commandUnits.forEach(commandUnit -> {
        commandUnit.setExecutionResult(logService.getUnitExecutionResult(appId, activityId, commandUnit.getName()));
      });
    }
    return commandUnits;
  }

  /**
   * Gets flatten command unit list.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param command   the command
   * @return the flatten command unit list
   */
  private List<CommandUnit> getFlattenCommandUnitList(String appId, String serviceId, Command command) {
    Command executableCommand = command;
    if (executableCommand == null) {
      return new ArrayList<>();
    }

    if (isNotBlank(command.getReferenceId())) {
      executableCommand = serviceResourceService.getCommandByName(appId, serviceId, command.getReferenceId());
      if (executableCommand == null) {
        throw new WingsException(COMMAND_DOES_NOT_EXIST);
      }
    }

    return executableCommand.getCommandUnits()
        .stream()
        .flatMap(commandUnit -> {
          if (COMMAND.equals(commandUnit.getCommandUnitType())) {
            return getFlattenCommandUnitList(appId, serviceId, (Command) commandUnit).stream();
          } else {
            return Stream.of(commandUnit);
          }
        })
        .collect(toList());
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
  public List<Artifact> getRecentArtifactsForInstance(String appId, String envId, String serviceInstanceId) {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", Operator.EQ, appId)
                                  .addFilter("environmentId", Operator.EQ, envId)
                                  .addFilter("serviceInstanceId", Operator.EQ, serviceInstanceId)
                                  .addFilter("artifactId", Operator.EXISTS)
                                  .withLimit("5")
                                  .build();
    List<Activity> activities = list(pageRequest).getResponse();
    return activities.stream()
        .map(activity -> artifactService.get(appId, activity.getArtifactId()))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public List<Activity> getRecentActivitiesForInstance(String appId, String envId, String serviceInstanceId) {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", Operator.EQ, appId)
                                  .addFilter("environmentId", Operator.EQ, envId)
                                  .addFilter("serviceInstanceId", Operator.EQ, serviceInstanceId)
                                  .withLimit("5")
                                  .build();
    return list(pageRequest).getResponse();
  }
}

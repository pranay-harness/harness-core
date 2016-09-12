package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Activity;
import software.wings.beans.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.validation.Create;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public interface ActivityService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Activity> list(PageRequest<Activity> pageRequest);

  /**
   * Gets the.
   *
   * @param id    the id
   * @param appId the app id
   * @return the activity
   */
  Activity get(String id, String appId);

  /**
   * Save.
   *
   * @param activity the activity
   * @return the activity
   */
  @ValidationGroups(Create.class) Activity save(@Valid Activity activity);

  /**
   * Update status.
   *
   * @param activityId     the activity id
   * @param appId          the app id
   * @param status the activity status
   */
  void updateStatus(String activityId, String appId, ExecutionStatus status);

  /**
   * Gets command units.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the command units
   */
  List<CommandUnit> getCommandUnits(String appId, String activityId);

  /**
   * Gets last activity for service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the last activity for service
   */
  Activity getLastActivityForService(String appId, String serviceId);

  /**
   * Gets lastproduction activity for service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the lastproduction activity for service
   */
  Activity getLastProductionActivityForService(String appId, String serviceId);

  /**
   * Gets recent artifacts for instance id.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceInstanceId the service instance id
   * @return the recent artifacts for instance id
   */
  List<Artifact> getRecentArtifactsForInstance(String appId, String envId, String serviceInstanceId);

  /**
   * Recent activities for instance list.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceInstanceId the service instance id
   * @return the list
   */
  List<Activity> getRecentActivitiesForInstance(String appId, String envId, String serviceInstanceId);
}

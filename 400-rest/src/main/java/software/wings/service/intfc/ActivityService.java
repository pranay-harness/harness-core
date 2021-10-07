package software.wings.service.intfc;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.validation.Create;

import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.command.CommandUnitDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
   * @param activityId the activity id
   * @param appId      the app id
   * @param status     the activity status
   */
  void updateStatus(String activityId, String appId, ExecutionStatus status);

  /**
   * Gets command units.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the command units
   */
  List<CommandUnitDetails> getCommandUnits(String appId, String activityId);

  Map<String, List<CommandUnitDetails>> getCommandUnitsMapUsingSecondary(Collection<String> activityIds);

  /**
   * Gets last activity for service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the last activity for service
   */
  Activity getLastActivityForService(String appId, String serviceId);

  /**
   * Get the list of activities which are related to previous workflow executions for the same service.
   *
   * @param appId the app id
   * @param serviceId the service id
   * @param workflowExecutionId the current workflow id
   * @param skip number of workflows to skip
   * @param limit limit numbers of workflows
   * @return the list of activities for rollback
   */
  List<Activity> getRollbackActivitiesForService(
      String appId, String serviceId, String workflowExecutionId, int skip, int limit);

  /**
   * Gets lastproduction activity for service.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the lastproduction activity for service
   */
  Activity getLastProductionActivityForService(String appId, String serviceId);

  /**
   * Delete boolean.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the boolean
   */
  boolean delete(String appId, String activityId);

  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String activityId);

  /**
   * Update command unit status.
   *
   * @param activityCommandUnitLastLogMap the activity command unit last log map
   */
  void updateCommandUnitStatus(Map<String, Map<String, Log>> activityCommandUnitLastLogMap);

  void updateCommandUnitStatus(
      String appId, String activityId, String unitName, CommandExecutionStatus commandUnitStatus);
}

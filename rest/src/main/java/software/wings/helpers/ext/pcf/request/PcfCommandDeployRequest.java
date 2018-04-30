package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.pcf.PcfServiceData;
import software.wings.beans.PcfConfig;
import software.wings.beans.ResizeStrategy;

import java.util.List;

/**
 * This class contains all required data for PCFCommandTask.DEPLOY to perform setup task
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PcfCommandDeployRequest extends PcfCommandRequest {
  private String newReleaseName;
  private List<String> routeMaps;

  /**
   * This is not desired count but update count, means upsize new app by currentCount + 5,
   * delegating calculating actual desiredInstanceCount to PCFCommandTask
   * (delegate), makes sure in all deploy state, we calculate based on most current data.
   *
   */
  private Integer updateCount;
  private Integer downSizeCount;
  private Integer totalPreviousInstanceCount;
  /**
   * This will be empty for deploy_state, so deploy will figureOut old versions and scale them down by 5
   * This will be set by Rollback, Rollback will use same request and PCFCommand.DEPLOY,
   * and looking at this list, we will know its coming from deploy state or rollback state
   */
  private List<PcfServiceData> instanceData;

  private ResizeStrategy resizeStrategy;

  @Builder
  public PcfCommandDeployRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      String newReleaseName, Integer updateCount, Integer downSizeCount, Integer totalPreviousInstanceCount,
      List<PcfServiceData> instanceData, ResizeStrategy resizeStrategy, List<String> routeMaps,
      Integer timeoutIntervalInMin) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.newReleaseName = newReleaseName;
    this.updateCount = updateCount;
    this.downSizeCount = downSizeCount;
    this.instanceData = instanceData;
    this.resizeStrategy = resizeStrategy;
    this.routeMaps = routeMaps;
    this.totalPreviousInstanceCount = totalPreviousInstanceCount;
  }
}

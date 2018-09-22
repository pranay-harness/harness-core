package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.PcfConfig;
import software.wings.beans.artifact.ArtifactFile;

import java.util.List;
import java.util.Map;

/**
 * This class contains all required data for PCFCommandTask.SETUP to perform setup task
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PcfCommandSetupRequest extends PcfCommandRequest {
  /**
   * releasePrefixName is (appId_serviceId_envId), while creating new version of app,
   * we will add 1 to most recent version deployed,
   * so actual app name will be appId_serviceId_envId__version
   */
  private String releaseNamePrefix;
  private String manifestYaml;
  private List<ArtifactFile> artifactFiles;
  private List<String> tempRouteMap;
  private List<String> routeMaps;
  private Map<String, String> serviceVariables;
  private Map<String, String> safeDisplayServiceVariables;
  private Integer maxCount;

  @Builder
  public PcfCommandSetupRequest(String accountId, String appId, String commandName, String activityId,
      PcfCommandType pcfCommandType, String organization, String space, PcfConfig pcfConfig, String workflowExecutionId,
      String releaseNamePrefix, String manifestYaml, List<ArtifactFile> artifactFiles, List<String> tempRouteMap,
      List<String> routeMaps, Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      Integer timeoutIntervalInMin, Integer maxCount) {
    super(accountId, appId, commandName, activityId, pcfCommandType, organization, space, pcfConfig,
        workflowExecutionId, timeoutIntervalInMin);
    this.releaseNamePrefix = releaseNamePrefix;
    this.manifestYaml = manifestYaml;
    this.artifactFiles = artifactFiles;
    this.tempRouteMap = tempRouteMap;
    this.routeMaps = routeMaps;
    this.serviceVariables = serviceVariables;
    this.safeDisplayServiceVariables = safeDisplayServiceVariables;
    this.maxCount = maxCount;
  }
}

package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.PcfManifestsPackage;

import software.wings.api.ExecutionDataValue;
import software.wings.api.pcf.PcfSetupExecutionSummary.PcfSetupExecutionSummaryBuilder;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class PcfSetupStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String accountId;
  private String appId;
  private String serviceId;
  private String envId;
  private String infraMappingId;
  private CfCommandRequest pcfCommandRequest;
  private String commandName;
  private Integer maxInstanceCount;
  private boolean useCurrentRunningInstanceCount;
  private Integer currentRunningInstanceCount;
  private List<String> routeMaps;
  private List<String> tempRouteMaps;
  private boolean rollback;
  private boolean isStandardBlueGreen;
  private boolean useTempRoutes;
  private TaskType taskType;
  private Map<K8sValuesLocation, ApplicationManifest> appManifestMap;
  private GitFetchFilesFromMultipleRepoResult fetchFilesResult;
  private boolean enforceSslValidation;
  private boolean useAppAutoscalar;
  private Integer timeout;
  private Integer activeVersionsToKeep;
  private String pcfAppNameFromLegacyWorkflow;
  private ResizeStrategy resizeStrategy;
  private Integer desireActualFinalCount;
  private PcfManifestsPackage pcfManifestsPackage;
  // This is just to preserve setupState data.
  private String[] tempRoutesOnSetupState;
  private String[] finalRoutesOnSetupState;
  private boolean useArtifactProcessingScript;
  private String artifactProcessingScript;
  private List<String> tags;
  private String cfAppNamePrefix;
  private boolean isNonVersioning;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();

    if (pcfCommandRequest != null) {
      putNotNull(executionDetails, "organization",
          ExecutionDataValue.builder().value(pcfCommandRequest.getOrganization()).displayName("Organization").build());
      putNotNull(executionDetails, "space",
          ExecutionDataValue.builder().value(pcfCommandRequest.getSpace()).displayName("Space").build());
    }

    List<String> urls = isStandardBlueGreen ? tempRouteMaps : routeMaps;
    if (isNotEmpty(urls)) {
      putNotNull(executionDetails, "routeMaps",
          ExecutionDataValue.builder().value(String.valueOf(urls)).displayName("Routes").build());
    }

    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return executionDetails;
  }

  @Override
  public PcfSetupExecutionSummary getStepExecutionSummary() {
    Integer count = useCurrentRunningInstanceCount ? currentRunningInstanceCount : maxInstanceCount;
    PcfSetupExecutionSummaryBuilder builder =
        PcfSetupExecutionSummary.builder().maxInstanceCount(count == null ? 0 : count);

    if (pcfCommandRequest != null) {
      builder.organization(pcfCommandRequest.getOrganization()).space(pcfCommandRequest.getSpace());
    }

    return builder.build();
  }
}

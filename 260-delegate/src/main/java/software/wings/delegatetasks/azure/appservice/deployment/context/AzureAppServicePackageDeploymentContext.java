package software.wings.delegatetasks.azure.appservice.deployment.context;

import static io.harness.azure.model.AzureConstants.ARTIFACT_FILE_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.ARTIFACT_TYPE_BLANK_ERROR_MSG;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import software.wings.delegatetasks.azure.arm.deployment.validator.ArtifactTypeSubset;
import software.wings.utils.ArtifactType;

import java.io.File;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureAppServicePackageDeploymentContext extends AzureAppServiceDeploymentContext {
  private String startupCommand;
  @NotNull(message = ARTIFACT_FILE_BLANK_ERROR_MSG) private File artifactFile;
  @NotNull(message = ARTIFACT_TYPE_BLANK_ERROR_MSG)
  @ArtifactTypeSubset(anyOf = {ArtifactType.ZIP, ArtifactType.WAR, ArtifactType.NUGET})
  private ArtifactType artifactType;

  @Builder
  public AzureAppServicePackageDeploymentContext(AzureWebClientContext azureWebClientContext,
      ILogStreamingTaskClient logStreamingTaskClient, Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd,
      Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove,
      Map<String, AzureAppServiceConnectionString> connSettingsToAdd,
      Map<String, AzureAppServiceConnectionString> connSettingsToRemove, String startupCommand, String slotName,
      String targetSlotName, File artifactFile, ArtifactType artifactType, int steadyStateTimeoutInMin) {
    super(azureWebClientContext, logStreamingTaskClient, appSettingsToAdd, appSettingsToRemove, connSettingsToAdd,
        connSettingsToRemove, slotName, targetSlotName, steadyStateTimeoutInMin);
    this.startupCommand = startupCommand;
    this.artifactFile = artifactFile;
    this.artifactType = artifactType;
  }
}

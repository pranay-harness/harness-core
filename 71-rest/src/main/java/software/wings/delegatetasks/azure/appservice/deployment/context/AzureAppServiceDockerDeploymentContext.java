package software.wings.delegatetasks.azure.appservice.deployment.context;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureAppServiceDockerDeploymentContext extends AzureAppServiceDeploymentContext {
  private String imagePathAndTag;
  private Map<String, AzureAppServiceDockerSetting> dockerSettings;

  @Builder
  AzureAppServiceDockerDeploymentContext(AzureWebClientContext azureWebClientContext,
      ILogStreamingTaskClient logStreamingTaskClient, Map<String, AzureAppServiceApplicationSetting> appSettings,
      Map<String, AzureAppServiceConnectionString> connSettings,
      Map<String, AzureAppServiceDockerSetting> dockerSettings, String slotName, String imagePathAndTag,
      int steadyStateTimeoutInMin) {
    super(azureWebClientContext, logStreamingTaskClient, appSettings, connSettings, slotName, steadyStateTimeoutInMin);
    this.dockerSettings = dockerSettings;
    this.imagePathAndTag = imagePathAndTag;
  }
}

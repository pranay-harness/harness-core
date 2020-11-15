package software.wings.delegatetasks.azure.appservice.deployment;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AzureAppServiceDeploymentContext {
  private AzureWebClientContext azureWebClientContext;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private Map<String, AzureAppServiceApplicationSetting> appSettings;
  private Map<String, AzureAppServiceConnectionString> connSettings;
  private Map<String, AzureAppServiceDockerSetting> dockerSettings;
  private String slotName;
  private String imageAndTag;
  private long slotStoppingSteadyStateTimeoutInMinutes;
  private long slotStartingSteadyStateTimeoutInMinutes;
}

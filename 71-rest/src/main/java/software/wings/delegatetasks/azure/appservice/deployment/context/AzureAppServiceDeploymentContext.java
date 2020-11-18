package software.wings.delegatetasks.azure.appservice.deployment.context;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class AzureAppServiceDeploymentContext {
  private AzureWebClientContext azureWebClientContext;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private Map<String, AzureAppServiceApplicationSetting> appSettings;
  private Map<String, AzureAppServiceConnectionString> connSettings;
  private String slotName;
  private int steadyStateTimeoutInMin;
}

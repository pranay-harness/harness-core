package software.wings.delegatetasks.azure.appservice.deployment.context;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureAppServiceDeploymentContext {
  private AzureWebClientContext azureWebClientContext;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd;
  private Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove;
  private Map<String, AzureAppServiceConnectionString> connSettingsToAdd;
  private Map<String, AzureAppServiceConnectionString> connSettingsToRemove;
  private String slotName;
  private int steadyStateTimeoutInMin;
}

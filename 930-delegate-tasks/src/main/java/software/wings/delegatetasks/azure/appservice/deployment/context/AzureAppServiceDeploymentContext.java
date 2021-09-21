package software.wings.delegatetasks.azure.appservice.deployment.context;

import static io.harness.azure.model.AzureConstants.SLOT_NAME_BLANK_ERROR_MSG;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AzureAppServiceDeploymentContext {
  private AzureWebClientContext azureWebClientContext;
  private ILogStreamingTaskClient logStreamingTaskClient;
  private Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd;
  private Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove;
  private Map<String, AzureAppServiceConnectionString> connSettingsToAdd;
  private Map<String, AzureAppServiceConnectionString> connSettingsToRemove;
  @NotBlank(message = SLOT_NAME_BLANK_ERROR_MSG) private String slotName;
  private String targetSlotName;
  private int steadyStateTimeoutInMin;
}

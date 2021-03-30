package io.harness.delegate.task.azure.appservice;

import static io.harness.azure.model.AzureConstants.SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;

import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceApplicationSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceConnectionStringDTO;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
public class AzureAppServicePreDeploymentData {
  @NotBlank(message = WEB_APP_NAME_BLANK_ERROR_MSG) private String appName;
  @NotBlank(message = SLOT_NAME_BLANK_ERROR_MSG) private String slotName;
  private double trafficWeight;
  private String deploymentProgressMarker;
  private Map<String, AzureAppServiceApplicationSettingDTO> appSettingsToRemove;
  private Map<String, AzureAppServiceApplicationSettingDTO> appSettingsToAdd;
  private Map<String, AzureAppServiceConnectionStringDTO> connStringsToRemove;
  private Map<String, AzureAppServiceConnectionStringDTO> connStringsToAdd;
  private Map<String, AzureAppServiceApplicationSettingDTO> dockerSettingsToAdd;
  private String imageNameAndTag;
  private AzureAppServiceTaskType failedTaskType;
}

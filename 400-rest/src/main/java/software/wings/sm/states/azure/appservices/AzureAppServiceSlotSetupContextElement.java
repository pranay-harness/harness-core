package software.wings.sm.states.azure.appservices;

import io.harness.context.ContextElementType;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.pms.sdk.core.data.SweepingOutput;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("azureAppServiceSlotSetupContextElement")
public class AzureAppServiceSlotSetupContextElement implements ContextElement, SweepingOutput {
  private String uuid;
  private String name;
  private String commandName;
  private String webApp;
  private String deploymentSlot;
  private Integer appServiceSlotSetupTimeOut;
  private String infraMappingId;
  private AzureAppServicePreDeploymentData preDeploymentData;
  public static final String AMI_SERVICE_SETUP_SWEEPING_OUTPUT_NAME = "setupSweepingOutputAppService";

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AZURE_WEBAPP_SETUP;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put("webApp", webApp);
    map.put("deploymentSlot", deploymentSlot);
    return ImmutableMap.of("azurewebapp", map);
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }

  @Override
  public String getType() {
    return "azureAppServiceSlotSetupContextElement";
  }
}

package software.wings.sm.states.azure.appservices;

import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.context.ContextElementType;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureAppServiceSlotSetupContextElement implements ContextElement {
  private String uuid;
  private String name;
  private String commandName;
  private String webApp;
  private String deploymentSlot;
  private Integer appServiceSlotSetupTimeOut;
  private String infraMappingId;
  private AzureAppServicePreDeploymentData preDeploymentData;

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
}

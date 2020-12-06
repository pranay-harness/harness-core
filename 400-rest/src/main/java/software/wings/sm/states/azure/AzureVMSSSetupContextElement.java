package software.wings.sm.states.azure;

import io.harness.context.ContextElementType;
import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;
import io.harness.delegate.task.azure.request.AzureLoadBalancerDetailForBGDeployment;

import software.wings.beans.ResizeStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
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
public class AzureVMSSSetupContextElement implements ContextElement {
  private String uuid;
  private String name;
  private String commandName;
  private int instanceCount;
  private String newVirtualMachineScaleSetName;
  private String oldVirtualMachineScaleSetName;
  private Integer autoScalingSteadyStateVMSSTimeout;
  private Integer maxInstances;
  private int oldDesiredCount;
  private int desiredInstances;
  private int minInstances;
  private boolean isBlueGreen;
  private String infraMappingId;
  private ResizeStrategy resizeStrategy;
  private List<String> baseVMSSScalingPolicyJSONs;
  private AzureVMSSPreDeploymentData preDeploymentData;
  private AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AZURE_VMSS_SETUP;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put("newVMSSName", newVirtualMachineScaleSetName);
    map.put("oldVMSSName", oldVirtualMachineScaleSetName);
    return ImmutableMap.of("azurevmss", map);
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}

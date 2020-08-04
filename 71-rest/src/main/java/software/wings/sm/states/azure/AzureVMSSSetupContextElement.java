package software.wings.sm.states.azure;

import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.context.ContextElementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.ResizeStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private int desiredInstances;
  private int minInstances;
  private List<String> oldVMSSNames;
  private boolean blueGreen;
  private ResizeStrategy resizeStrategy;
  private List<String> baseVMSSScalingPolicyJSONs;

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

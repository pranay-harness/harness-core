package software.wings.sm.states.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureVMSSDeployExecutionSummary extends StepExecutionSummary {
  private String oldVirtualMachineScaleSetId;
  private String oldVirtualMachineScaleSetName;
  private String newVirtualMachineScaleSetId;
  private String newVirtualMachineScaleSetName;
}

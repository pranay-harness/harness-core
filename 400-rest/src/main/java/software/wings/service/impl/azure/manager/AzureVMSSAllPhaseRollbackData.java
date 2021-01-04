package software.wings.service.impl.azure.manager;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("azureVMSSAllPhaseRollbackData")
public class AzureVMSSAllPhaseRollbackData implements SweepingOutput {
  public static final String AZURE_VMSS_ALL_PHASE_ROLLBACK = "Azure VMSS all phase rollback";
  boolean allPhaseRollbackDone;

  @Override
  public String getType() {
    return "azureVMSSAllPhaseRollbackData";
  }
}

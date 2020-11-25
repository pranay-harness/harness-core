package io.harness.delegate.task.azure.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.azure.AzureVMSSPreDeploymentData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSSetupTaskResponse implements AzureVMSSTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private String newVirtualMachineScaleSetName;
  private String lastDeployedVMSSName;
  private Integer harnessRevision;
  private boolean blueGreen;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private List<String> baseVMSSScalingPolicyJSONs;
  private AzureVMSSPreDeploymentData preDeploymentData;
}

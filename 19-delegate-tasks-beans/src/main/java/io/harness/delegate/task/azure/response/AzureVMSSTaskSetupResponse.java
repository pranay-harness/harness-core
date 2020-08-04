package io.harness.delegate.task.azure.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSTaskSetupResponse implements AzureVMSSTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private String newVirtualMachineScaleSetName;
  private String lastDeployedVMSSName;
  private Integer harnessRevision;
  private List<String> oldVMSSNames;
  private boolean blueGreen;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private List<String> baseVMSSScalingPolicyJSONs;
}

package io.harness.delegate.task.azure.response;

import io.harness.azure.model.AzureVMData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSListVMDataResponse implements AzureVMSSTaskResponse {
  private String vmssId;
  private List<AzureVMData> vmData;
}

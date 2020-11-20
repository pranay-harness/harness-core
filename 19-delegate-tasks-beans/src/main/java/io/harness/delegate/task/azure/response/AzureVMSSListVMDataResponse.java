package io.harness.delegate.task.azure.response;

import io.harness.azure.model.AzureVMData;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureVMSSListVMDataResponse implements AzureVMSSTaskResponse {
  private String vmssId;
  private List<AzureVMData> vmData;
}

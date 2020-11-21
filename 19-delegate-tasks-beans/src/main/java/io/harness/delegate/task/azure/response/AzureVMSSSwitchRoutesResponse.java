package io.harness.delegate.task.azure.response;

import io.harness.delegate.beans.DelegateMetaInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSSwitchRoutesResponse implements AzureVMSSTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
}

package io.harness.delegate.task.azure.arm.response;

import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMListManagementGroupNamesResponse extends AzureARMTaskResponse {
  private List<String> mngGroupNames;

  @Builder
  public AzureARMListManagementGroupNamesResponse(List<String> mngGroupNames, String errorMsg) {
    super(errorMsg);
    this.mngGroupNames = mngGroupNames;
  }
}

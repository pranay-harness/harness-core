package io.harness.delegate.task.azure.arm.response;

import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMListSubscriptionLocationsResponse extends AzureARMTaskResponse {
  private List<String> locations;

  @Builder
  public AzureARMListSubscriptionLocationsResponse(List<String> locations, String errorMsg) {
    super(errorMsg);
    this.locations = locations;
  }
}

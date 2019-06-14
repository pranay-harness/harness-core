package software.wings.helpers.ext.helm.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;

@Data
@Builder
public class HelmValuesFetchTaskParameters implements ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String activityId;
  private String workflowExecutionId;

  // This is to support helm v1
  private ContainerServiceParams containerServiceParams;
  private String helmCommandFlags;

  private HelmChartConfigParams helmChartConfigTaskParams;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return helmChartConfigTaskParams.fetchRequiredExecutionCapabilities();
  }
}

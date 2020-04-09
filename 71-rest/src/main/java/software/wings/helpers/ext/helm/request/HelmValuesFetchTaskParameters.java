package software.wings.helpers.ext.helm.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.delegatetasks.validation.capabilities.HelmCommandCapability;
import software.wings.service.impl.ContainerServiceParams;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class HelmValuesFetchTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String activityId;
  private String workflowExecutionId;
  private boolean isBindTaskFeatureSet; // BIND_FETCH_FILES_TASK_TO_DELEGATE

  // This is to support helm v1
  private ContainerServiceParams containerServiceParams;
  private String helmCommandFlags;

  private HelmChartConfigParams helmChartConfigTaskParams;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    HelmInstallCommandRequest commandRequest = HelmInstallCommandRequest.builder()
                                                   .commandFlags(getHelmCommandFlags())
                                                   .helmVersion(getHelmChartConfigTaskParams().getHelmVersion())
                                                   .build();
    return Collections.singletonList(HelmCommandCapability.builder().commandRequest(commandRequest).build());
  }
}

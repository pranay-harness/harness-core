package software.wings.helpers.ext.helm.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;

import software.wings.delegatetasks.validation.capabilities.HelmCommandCapability;
import software.wings.service.impl.ContainerServiceParams;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmValuesFetchTaskParameters implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String activityId;
  private String workflowExecutionId;
  private boolean isBindTaskFeatureSet; // BIND_FETCH_FILES_TASK_TO_DELEGATE
  private long timeoutInMillis;

  // This is to support helm v1
  private ContainerServiceParams containerServiceParams;
  @Expression(ALLOW_SECRETS) private String helmCommandFlags;

  private HelmChartConfigParams helmChartConfigTaskParams;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> capabilities = new ArrayList<>();

    if (helmChartConfigTaskParams != null && helmChartConfigTaskParams.getHelmRepoConfig() != null) {
      capabilities.addAll(helmChartConfigTaskParams.fetchRequiredExecutionCapabilities());
      if (isBindTaskFeatureSet && containerServiceParams != null) {
        capabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities());
      }
    } else {
      capabilities.add(HelmCommandCapability.builder()
                           .commandRequest(HelmInstallCommandRequest.builder()
                                               .commandFlags(getHelmCommandFlags())
                                               .helmVersion(getHelmChartConfigTaskParams().getHelmVersion())
                                               .containerServiceParams(getContainerServiceParams())
                                               .build())
                           .build());

      if (containerServiceParams != null) {
        capabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities());
      }
    }

    return capabilities;
  }
}

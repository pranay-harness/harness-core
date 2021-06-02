package software.wings.helpers.ext.helm.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;

import software.wings.delegatetasks.validation.capabilities.HelmCommandCapability;
import software.wings.service.impl.ContainerServiceParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class HelmValuesFetchTaskParameters implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String activityId;
  private String workflowExecutionId;
  private boolean isBindTaskFeatureSet; // BIND_FETCH_FILES_TASK_TO_DELEGATE
  private long timeoutInMillis;
  @Expression(ALLOW_SECRETS) private HelmCommandFlag helmCommandFlag;
  private boolean mergeCapabilities; // HELM_MERGE_CAPABILITIES
  private Set<String> delegateSelectors;

  // This is to support helm v1
  private ContainerServiceParams containerServiceParams;
  @Expression(ALLOW_SECRETS) private String helmCommandFlags;

  private HelmChartConfigParams helmChartConfigTaskParams;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities = new ArrayList<>();

    if (helmChartConfigTaskParams != null && helmChartConfigTaskParams.getHelmRepoConfig() != null) {
      capabilities.addAll(helmChartConfigTaskParams.fetchRequiredExecutionCapabilities(maskingEvaluator));
      if (isBindTaskFeatureSet && containerServiceParams != null) {
        capabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities(maskingEvaluator));
      }
    } else {
      if (mergeCapabilities) {
        capabilities.add(HelmInstallationCapability.builder()
                             .version(getHelmChartConfigTaskParams().getHelmVersion())
                             .criteria("helmcommand")
                             .build());
      } else {
        capabilities.add(HelmCommandCapability.builder()
                             .commandRequest(HelmInstallCommandRequest.builder()
                                                 .commandFlags(getHelmCommandFlags())
                                                 .helmCommandFlag(getHelmCommandFlag())
                                                 .helmVersion(getHelmChartConfigTaskParams().getHelmVersion())
                                                 .containerServiceParams(getContainerServiceParams())
                                                 .build())
                             .build());
      }

      if (containerServiceParams != null) {
        capabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities(maskingEvaluator));
      }

      if (isNotEmpty(delegateSelectors)) {
        capabilities.add(SelectorCapability.builder().selectors(delegateSelectors).build());
      }
    }

    return capabilities;
  }
}

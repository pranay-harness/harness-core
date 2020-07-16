package software.wings.helpers.ext.k8s.request;

import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.expression.Expression;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class K8sTaskParameters implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private K8sClusterConfig k8sClusterConfig;
  private String workflowExecutionId;
  @Expression private String releaseName;
  private Integer timeoutIntervalInMin;
  @NotEmpty private K8sTaskType commandType;
  private HelmVersion helmVersion;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.addAll(k8sClusterConfig.fetchRequiredExecutionCapabilities());
    if (kustomizeValidationNeeded()) {
      executionCapabilities.add(
          KustomizeCapability.builder().kustomizeConfig(fetchKustomizeConfig((ManifestAwareTaskParams) this)).build());
    }

    return executionCapabilities;
  }

  private boolean kustomizeValidationNeeded() {
    if (this instanceof ManifestAwareTaskParams) {
      return fetchKustomizeConfig((ManifestAwareTaskParams) this) != null;
    }
    return false;
  }

  private KustomizeConfig fetchKustomizeConfig(ManifestAwareTaskParams taskParams) {
    return taskParams.getK8sDelegateManifestConfig() != null
        ? taskParams.getK8sDelegateManifestConfig().getKustomizeConfig()
        : null;
  }
}

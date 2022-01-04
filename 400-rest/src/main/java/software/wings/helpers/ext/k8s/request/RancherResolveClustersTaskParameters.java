package software.wings.helpers.ext.k8s.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.RancherConfig;
import software.wings.infra.RancherKubernetesInfrastructure;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class RancherResolveClustersTaskParameters
    implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  private RancherConfig rancherConfig;
  private List<RancherKubernetesInfrastructure.ClusterSelectionCriteriaEntry> clusterSelectionCriteria;
  private String activityId;

  List<EncryptedDataDetail> encryptedDataDetails;

  @Builder
  public RancherResolveClustersTaskParameters(RancherConfig rancherConfig,
      List<RancherKubernetesInfrastructure.ClusterSelectionCriteriaEntry> clusterSelectionCriteria,
      List<EncryptedDataDetail> encryptedDataDetails, String activityId) {
    this.rancherConfig = rancherConfig;
    this.clusterSelectionCriteria = clusterSelectionCriteria;
    this.encryptedDataDetails = encryptedDataDetails;
    this.activityId = activityId;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return this.rancherConfig.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}

package software.wings.sm.states.argo;

import io.harness.beans.EnvironmentType;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.spotinst.model.ElastiGroup;

import software.wings.api.ExecutionDataValue;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ArgoStepExecutionSummary extends StepExecutionSummary {
  private String argoServerUrl;
  private String argoAppName;
  private String repoName;
  private String repoRef;
  private String syncOption;
}

package software.wings.beans.container;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import software.wings.service.impl.ContainerServiceParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class KubernetesSwapServiceSelectorsParams implements ExecutionCapabilityDemander, TaskParameters {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private ContainerServiceParams containerServiceParams;
  private String service1;
  private String service2;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return containerServiceParams.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}

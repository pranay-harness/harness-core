package io.harness.delegate.beans.ci;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.LiteEngineConnectionCapability;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8ExecuteStepTaskParams implements CIExecuteStepTaskParams, ExecutionCapabilityDemander {
  @NotNull private String ip;
  @NotNull private int port;
  @NotNull private String delegateSvcEndpoint;
  private boolean isLocal;
  @NotNull private byte[] serializedStep;
  private String podName;

  @Builder.Default private static final CIExecuteStepTaskParams.Type type = CIExecuteStepTaskParams.Type.K8;

  @Override
  public CIExecuteStepTaskParams.Type getType() {
    return type;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(
        LiteEngineConnectionCapability.builder().ip(ip).port(port).isLocal(isLocal).build());
  }
}

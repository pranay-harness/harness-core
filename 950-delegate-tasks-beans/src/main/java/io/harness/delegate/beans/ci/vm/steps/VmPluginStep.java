package io.harness.delegate.beans.ci.vm.steps;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.EnvVariableEnum;
import io.harness.expression.Expression;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VmPluginStep implements VmStepInfo {
  private String image;
  private ConnectorDetails imageConnector;
  private ConnectorDetails connector;
  private Map<EnvVariableEnum, String> connectorSecretEnvMap;
  private String pullPolicy;
  private boolean privileged;
  private String runAsUser;

  @Expression(ALLOW_SECRETS) private Map<String, String> envVariables;
  private VmUnitTestReport unitTestReport;
  private long timeoutSecs;

  @Override
  public VmStepInfo.Type getType() {
    return VmStepInfo.Type.PLUGIN;
  }
}

package io.harness.delegate.beans.ci.vm.steps;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.expression.Expression;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VmRunTestStep implements VmStepInfo {
  private String image;
  private ConnectorDetails connector;
  private String pullPolicy;
  private boolean privileged;
  private String runAsUser;

  @Expression(ALLOW_SECRETS) private String args;
  private List<String> entrypoint;
  private String language;
  private String buildTool;
  private String packages;
  private String testAnnotations;
  private boolean runOnlySelectedTests;
  @Expression(ALLOW_SECRETS) private String preCommand;
  @Expression(ALLOW_SECRETS) private String postCommand;
  @Expression(ALLOW_SECRETS) private Map<String, String> envVariables;
  private List<String> outputVariables;
  private VmUnitTestReport unitTestReport;
  private long timeoutSecs;

  @Override
  public VmStepInfo.Type getType() {
    return VmStepInfo.Type.RUN_TEST;
  }
}

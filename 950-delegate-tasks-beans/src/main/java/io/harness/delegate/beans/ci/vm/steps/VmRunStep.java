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
public class VmRunStep implements VmStepInfo {
  private String image;
  private ConnectorDetails imageConnector;
  private String pullPolicy; // always, if-not-exists or never
  private String runAsUser;
  private boolean privileged;

  private List<String> entrypoint;
  @Expression(ALLOW_SECRETS) private String command;
  private List<String> outputVariables;
  @Expression(ALLOW_SECRETS) private Map<String, String> envVariables;
  private VmUnitTestReport unitTestReport;
  private long timeoutSecs;

  @Override
  public VmStepInfo.Type getType() {
    return Type.RUN;
  }
}

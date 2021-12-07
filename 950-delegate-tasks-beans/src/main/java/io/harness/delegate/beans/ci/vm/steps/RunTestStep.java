package io.harness.delegate.beans.ci.vm.steps;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@Value
public class RunTestStep implements StepInfo {
  private String image;
  private ConnectorDetails connector;
  private String pullPolicy;
  private boolean privileged;
  private String runAsUser;

  private String args;
  private List<String> entrypoint;
  private String language;
  private String buildTool;
  private String packages;
  private String testAnnotations;
  private boolean runOnlySelectedTests;
  private String preCommand;
  private String postCommand;
  private Map<String, String> envVariables;
  private List<String> outputVariables;
  private UnitTestReport unitTestReport;

  @Override
  public StepInfo.Type getType() {
    return StepInfo.Type.RUN_TEST;
  }
}

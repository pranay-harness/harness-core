package io.harness.delegate.beans.ci.vm.steps;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@Value
public class PluginStep implements StepInfo {
  private String image;
  private ConnectorDetails connector;
  private String pullPolicy;
  private boolean privileged;
  private String runAsUser;

  private Map<String, String> envVariables;
  private UnitTestReport unitTestReport;

  @Override
  public StepInfo.Type getType() {
    return StepInfo.Type.PLUGIN;
  }
}

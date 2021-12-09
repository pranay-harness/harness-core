package io.harness.delegate.beans.ci.vm.steps;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@Value
public class VmPluginStep implements VmStepInfo {
  private String image;
  private ConnectorDetails connector;
  private String pullPolicy;
  private boolean privileged;
  private String runAsUser;

  private Map<String, String> envVariables;
  private VmUnitTestReport unitTestReport;
  private int timeoutSecs;

  @Override
  public VmStepInfo.Type getType() {
    return VmStepInfo.Type.PLUGIN;
  }
}

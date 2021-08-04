package io.harness.beans.plugin.compatible;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIStepInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.container.ContainerResource;

@OwnedBy(CI)
public interface PluginCompatibleStep extends CIStepInfo {
  // Common for all plugin compatible step types
  ParameterField<String> getConnectorRef();
  ContainerResource getResources();
  ParameterField<Integer> getRunAsUser();
}

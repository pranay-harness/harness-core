package io.harness.beans.plugin.compatible;

import io.harness.beans.steps.CIStepInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.container.ContainerResource;

public interface PluginCompatibleStep extends CIStepInfo {
  // Common for all plugin compatible step types
  ParameterField<String> getConnectorRef();
  ContainerResource getResources();
  ParameterField<Integer> getRunAsUser();
}

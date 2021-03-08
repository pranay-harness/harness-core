package io.harness.beans.plugin.compatible;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface PluginCompatibleStep extends CIStepInfo {
  // Common for all plugin compatible step types
  ParameterField<String> getConnectorRef();
  @JsonIgnore ParameterField<String> getContainerImage();
  ContainerResource getResources();
}

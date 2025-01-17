/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.plugin;

import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.WithDelegateSelector;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.plugin.infrastructure.ContainerStepInfra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public interface ContainerStepSpec extends WithDelegateSelector {
  void setName(String name);
  void setIdentifier(String identifier);
  ContainerStepInfra getInfrastructure();
  @JsonIgnore String getIdentifier();
  @JsonIgnore String getName();

  @JsonIgnore ContainerStepType getType();

  @Override
  default ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return ParameterField.ofNull();
  }

  @Override
  default void setDelegateSelectors(ParameterField<List<TaskSelectorYaml>> delegateSelectors) {}
}

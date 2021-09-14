/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filters;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@RecasterAlias("io.harness.filters.TestObjectWithConnectorRef")
public class TestObjectWithConnectorRef implements PMSStepInfo, WithConnectorRef {
  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> map = new HashMap<>();
    map.put(YAMLFieldNameConstants.CONNECTOR_REF, ParameterField.createValueField("connectorRef"));
    return map;
  }

  @Override
  public StepType getStepType() {
    return StepType.newBuilder().setType("Dummy").setStepCategory(StepCategory.STEP).build();
  }

  @Override
  public String getFacilitatorType() {
    return null;
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.pipeline.steps;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.TimeoutUtils;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class CdStepParametersUtils {
  public StepElementParametersBuilder getStepParameters(CdAbstractStepNode stepNode) {
    StepElementParametersBuilder stepBuilder = StepElementParameters.builder();
    stepBuilder.name(stepNode.getName());
    stepBuilder.identifier(stepNode.getIdentifier());
    stepBuilder.delegateSelectors(stepNode.getDelegateSelectors());
    stepBuilder.description(stepNode.getDescription());
    stepBuilder.skipCondition(stepNode.getSkipCondition());
    stepBuilder.failureStrategies(stepNode.getFailureStrategies());
    stepBuilder.timeout(ParameterField.createValueField(TimeoutUtils.getTimeoutString(stepNode.getTimeout())));
    stepBuilder.when(stepNode.getWhen());
    stepBuilder.type(stepNode.getType());
    stepBuilder.uuid(stepNode.getUuid());

    return stepBuilder;
  }
  public StepElementParametersBuilder getStepParameters(
      CdAbstractStepNode stepNode, OnFailRollbackParameters failRollbackParameters) {
    StepElementParametersBuilder stepBuilder = getStepParameters(stepNode);
    stepBuilder.rollbackParameters(failRollbackParameters);
    return stepBuilder;
  }
}

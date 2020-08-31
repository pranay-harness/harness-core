package io.harness.state.core.section.chain;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.expression.ExpressionEvaluator;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;

import java.util.Map;

public class RollbackOptionalChildChainStep
    implements Step, ChildChainExecutable<RollbackOptionalChildChainStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("SECTION_OPTIONAL_CHAIN").build();

  @Inject private EngineExpressionService engineExpressionService;

  @Override
  public ChildChainResponse executeFirstChild(
      Ambiance ambiance, RollbackOptionalChildChainStepParameters stepParameters, StepInputPackage inputPackage) {
    int index = 0;
    for (int i = index; i < stepParameters.getChildNodes().size(); i++) {
      RollbackOptionalChildChainStepParameters.Node childNode = stepParameters.getChildNodes().get(i);

      String value = engineExpressionService.renderExpression(
          ambiance, format("${%s.status}", childNode.getDependentNodeIdentifier()));

      if (!ExpressionEvaluator.containsVariablePattern(value)) {
        return ChildChainResponse.builder()
            .nextChildId(childNode.getNodeId())
            .passThroughData(SectionChainPassThroughData.builder().childIndex(i).build())
            .lastLink(stepParameters.getChildNodes().size() == i + 1)
            .build();
      }
    }
    return ChildChainResponse.builder().suspend(true).build();
  }

  @Override
  public ChildChainResponse executeNextChild(Ambiance ambiance, RollbackOptionalChildChainStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    int index = ((SectionChainPassThroughData) passThroughData).childIndex + 1;

    for (int i = index; i < stepParameters.getChildNodes().size(); i++) {
      RollbackOptionalChildChainStepParameters.Node childNode = stepParameters.getChildNodes().get(i);

      String value = engineExpressionService.renderExpression(
          ambiance, format("${%s.status}", childNode.getDependentNodeIdentifier()));

      if (!ExpressionEvaluator.containsVariablePattern(value)) {
        return ChildChainResponse.builder()
            .nextChildId(childNode.getNodeId())
            .passThroughData(SectionChainPassThroughData.builder().childIndex(i).build())
            .lastLink(stepParameters.getChildNodes().size() == i + 1)
            .previousChildId(responseDataMap.keySet().iterator().next())
            .build();
      }
    }
    return ChildChainResponse.builder().suspend(true).build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, RollbackOptionalChildChainStepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    StepResponseNotifyData notifyData = (StepResponseNotifyData) responseDataMap.values().iterator().next();
    return StepResponse.builder().status(notifyData.getStatus()).failureInfo(notifyData.getFailureInfo()).build();
  }
}

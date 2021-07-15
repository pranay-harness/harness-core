package io.harness.cdng.creator.plan.steps;

import static io.harness.cdng.visitor.YamlTypes.K8S_ROLLING_DEPLOY;
import static io.harness.pms.yaml.YAMLFieldNameConstants.EXECUTION;
import static io.harness.pms.yaml.YAMLFieldNameConstants.PARALLEL;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP_GROUP;

import static java.lang.String.format;

import io.harness.advisers.manualIntervention.ManualInterventionAdviserRollbackParameters;
import io.harness.advisers.manualIntervention.ManualInterventionAdviserWithRollback;
import io.harness.advisers.retry.RetryAdviserRollbackParameters;
import io.harness.advisers.retry.RetryAdviserWithRollback;
import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sRollingRollbackStepParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.WithStepElementParameters;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDP)
public class K8sRollingRollbackPMSStepPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("K8sRollingRollback");
  }

  private String getRollingStepFqn(YamlField currentField) {
    String rollingFqn = null;
    YamlNode execution = YamlUtils.findParentNode(currentField.getNode(), EXECUTION);
    List<YamlNode> steps = execution.getField(STEPS).getNode().asArray();
    for (YamlNode stepsNode : steps) {
      YamlNode stepGroup = getStepGroup(stepsNode);
      rollingFqn = stepGroup != null ? getRollingFqnFromStepGroup(stepGroup) : getRollingFqnFromStep(stepsNode);
      if (rollingFqn != null) {
        return rollingFqn;
      }
    }

    if (rollingFqn == null) {
      throw new InvalidRequestException(format("Failed to determine [%s] step path.", K8S_ROLLING_DEPLOY));
    }

    return rollingFqn;
  }

  private String getRollingFqnFromStepGroup(YamlNode stepGroup) {
    List<YamlNode> stepsInsideStepGroup = stepGroup.getField(STEPS).getNode().asArray();
    for (YamlNode stepsNodeInsideStepGroup : stepsInsideStepGroup) {
      YamlNode parallelStepNode = getParallelStep(stepsNodeInsideStepGroup);
      return parallelStepNode != null ? getRollingFqnFromParallelNode(parallelStepNode)
                                      : getFqnFromRollingStepNode(stepsNodeInsideStepGroup);
    }

    return null;
  }

  private String getRollingFqnFromStep(YamlNode stepsNode) {
    YamlNode parallelStepNode = getParallelStep(stepsNode);
    return parallelStepNode != null ? getRollingFqnFromParallelNode(parallelStepNode)
                                    : getFqnFromRollingStepNode(stepsNode);
  }

  private String getRollingFqnFromParallelNode(YamlNode parallelStepNode) {
    List<YamlNode> stepsInParallelNode = parallelStepNode.asArray();
    for (YamlNode stepInParallelNode : stepsInParallelNode) {
      return getFqnFromRollingStepNode(stepInParallelNode);
    }

    return null;
  }

  private String getFqnFromRollingStepNode(YamlNode stepsNode) {
    YamlNode stepNode = stepsNode.getField(STEP).getNode();
    if (K8S_ROLLING_DEPLOY.equals(stepNode.getType())) {
      return YamlUtils.getFullyQualifiedName(stepNode);
    }

    return null;
  }

  private YamlNode getStepGroup(YamlNode stepsNode) {
    try {
      return stepsNode.getField(STEP_GROUP).getNode();
    } catch (Exception ex) {
      return null;
    }
  }

  private YamlNode getParallelStep(YamlNode stepsNode) {
    try {
      return stepsNode.getField(PARALLEL).getNode();
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  protected AdviserObtainment getRetryAdviserObtainment(Set<FailureType> failureTypes, String nextNodeUuid,
      AdviserObtainment.Builder adviserObtainmentBuilder, RetryFailureActionConfig retryAction,
      ParameterField<Integer> retryCount, FailureStrategyActionConfig actionUnderRetry, YamlField currentField) {
    return adviserObtainmentBuilder.setType(RetryAdviserWithRollback.ADVISER_TYPE)
        .setParameters(ByteString.copyFrom(
            kryoSerializer.asBytes(RetryAdviserRollbackParameters.builder()
                                       .applicableFailureTypes(failureTypes)
                                       .nextNodeId(nextNodeUuid)
                                       .repairActionCodeAfterRetry(toRepairAction(actionUnderRetry))
                                       .retryCount(retryCount.getValue())
                                       .strategyToUuid(getRollbackStrategyMap(currentField))
                                       .waitIntervalList(retryAction.getSpecConfig()
                                                             .getRetryIntervals()
                                                             .getValue()
                                                             .stream()
                                                             .map(s -> (int) TimeoutUtils.getTimeoutInSeconds(s, 0))
                                                             .collect(Collectors.toList()))
                                       .build())))
        .build();
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, StepElementConfig stepElement) {
    StepParameters stepParameters = stepElement.getStepSpecType().getStepParameters();
    if (stepElement.getStepSpecType() instanceof WithStepElementParameters) {
      stepElement.setTimeout(TimeoutUtils.getTimeout(stepElement.getTimeout()));
      stepParameters =
          ((WithStepElementParameters) stepElement.getStepSpecType())
              .getStepParametersInfo(stepElement,
                  getRollbackParameters(ctx.getCurrentField(), Collections.emptySet(), RollbackStrategy.UNKNOWN));
    }

    String rollingFqn = getRollingStepFqn(ctx.getCurrentField());
    ((K8sRollingRollbackStepParameters) ((StepElementParameters) stepParameters).getSpec())
        .setRollingStepFqn(rollingFqn);

    return stepParameters;
  }

  @Override
  protected AdviserObtainment getManualInterventionAdviserObtainment(Set<FailureType> failureTypes,
      AdviserObtainment.Builder adviserObtainmentBuilder, ManualInterventionFailureActionConfig actionConfig,
      FailureStrategyActionConfig actionUnderManualIntervention, YamlField currentField) {
    return adviserObtainmentBuilder.setType(ManualInterventionAdviserWithRollback.ADVISER_TYPE)
        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
            ManualInterventionAdviserRollbackParameters.builder()
                .applicableFailureTypes(failureTypes)
                .timeoutAction(toRepairAction(actionUnderManualIntervention))
                .timeout((int) TimeoutUtils.getTimeoutInSeconds(actionConfig.getSpecConfig().getTimeout(), 0))
                .build())))
        .build();
  }
}

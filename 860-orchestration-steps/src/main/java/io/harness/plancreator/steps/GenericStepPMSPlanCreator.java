package io.harness.plancreator.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.FAILURE_STRATEGIES;
import static io.harness.pms.yaml.YAMLFieldNameConstants.PARALLEL;
import static io.harness.pms.yaml.YAMLFieldNameConstants.ROLLBACK_STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP_GROUP;

import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.advisers.rollback.OnFailRollbackAdviser;
import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.advisers.rollback.OnFailRollbackParameters.OnFailRollbackParametersBuilder;
import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.govern.Switch;
import io.harness.plancreator.beans.OrchestrationConstants;
import io.harness.plancreator.steps.common.WithStepElementParameters;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviser;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviserParameters;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviser;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviserParameters;
import io.harness.pms.sdk.core.adviser.manualintervention.ManualInterventionAdviser;
import io.harness.pms.sdk.core.adviser.manualintervention.ManualInterventionAdviserParameters;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviser;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviser;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;
import io.harness.timeout.TimeoutParameters;
import io.harness.timeout.contracts.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;
import io.harness.when.utils.RunInfoUtils;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureTypeConstants;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(PIPELINE)
public abstract class GenericStepPMSPlanCreator implements PartialPlanCreator<StepElementConfig> {
  @Inject protected KryoSerializer kryoSerializer;

  public abstract Set<String> getSupportedStepTypes();

  @Override
  public Class<StepElementConfig> getFieldClass() {
    return StepElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    Set<String> stepTypes = getSupportedStepTypes();
    if (EmptyPredicate.isEmpty(stepTypes)) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap(STEP, stepTypes);
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, StepElementConfig stepElement) {
    StepParameters stepParameters = stepElement.getStepSpecType().getStepParameters();

    boolean isStepInsideRollback = false;
    if (YamlUtils.findParentNode(ctx.getCurrentField().getNode(), ROLLBACK_STEPS) != null) {
      isStepInsideRollback = true;
    }

    List<AdviserObtainment> adviserObtainmentFromMetaData = getAdviserObtainmentFromMetaData(ctx.getCurrentField());

    if (stepElement.getStepSpecType() instanceof WithStepElementParameters) {
      stepElement.setTimeout(TimeoutUtils.getTimeout(stepElement.getTimeout()));
      stepParameters =
          ((WithStepElementParameters) stepElement.getStepSpecType())
              .getStepParametersInfo(stepElement,
                  getRollbackParameters(ctx.getCurrentField(), Collections.emptySet(), RollbackStrategy.UNKNOWN));
    }

    PlanNode stepPlanNode =
        PlanNode.builder()
            .uuid(ctx.getCurrentField().getNode().getUuid())
            .name(getName(stepElement))
            .identifier(stepElement.getIdentifier())
            .stepType(stepElement.getStepSpecType().getStepType())
            .group(StepOutcomeGroup.STEP.name())
            .stepParameters(stepParameters)
            .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder()
                                                    .setType(stepElement.getStepSpecType().getFacilitatorType())
                                                    .build())
                                       .build())
            .adviserObtainments(adviserObtainmentFromMetaData)
            .skipCondition(SkipInfoUtils.getSkipCondition(stepElement.getSkipCondition()))
            .whenCondition(isStepInsideRollback ? RunInfoUtils.getRunConditionForRollback(stepElement.getWhen())
                                                : RunInfoUtils.getRunCondition(stepElement.getWhen()))
            .timeoutObtainment(
                TimeoutObtainment.newBuilder()
                    .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
                    .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                        AbsoluteTimeoutParameters.builder().timeoutMillis(getTimeoutInMillis(stepElement)).build())))
                    .build())
            .build();
    return PlanCreationResponse.builder().node(stepPlanNode.getUuid(), stepPlanNode).build();
  }

  public static boolean containsOnlyAnyOtherErrorInSomeConfig(List<FailureStrategyConfig> stageFailureStrategies) {
    boolean containsOnlyAnyOther = false;
    for (FailureStrategyConfig failureStrategyConfig : stageFailureStrategies) {
      if (failureStrategyConfig.getOnFailure().getErrors().size() == 1
          && failureStrategyConfig.getOnFailure().getErrors().get(0).getYamlName().contentEquals(
              NGFailureTypeConstants.ANY_OTHER_ERRORS)) {
        containsOnlyAnyOther = true;
      }
    }
    return containsOnlyAnyOther;
  }

  protected String getName(StepElementConfig stepElement) {
    String nodeName;
    if (EmptyPredicate.isEmpty(stepElement.getName())) {
      nodeName = stepElement.getIdentifier();
    } else {
      nodeName = stepElement.getName();
    }
    return nodeName;
  }

  protected long getTimeoutInMillis(StepElementConfig stepElement) {
    long timeoutInMillis;
    if (ParameterField.isNull(stepElement.getTimeout())) {
      timeoutInMillis = TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
    } else {
      timeoutInMillis = stepElement.getTimeout().getValue().getTimeoutInMillis();
    }
    return timeoutInMillis;
  }

  protected List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainmentList = new ArrayList<>();

    boolean isStepInsideRollback = false;
    if (YamlUtils.findParentNode(currentField.getNode(), ROLLBACK_STEPS) != null) {
      isStepInsideRollback = true;
    }

    List<FailureStrategyConfig> stageFailureStrategies =
        getFieldFailureStrategies(currentField, STAGE, isStepInsideRollback);
    List<FailureStrategyConfig> stepGroupFailureStrategies =
        getFieldFailureStrategies(currentField, STEP_GROUP, isStepInsideRollback);
    List<FailureStrategyConfig> stepFailureStrategies = getFailureStrategies(currentField.getNode());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMap;
    FailureStrategiesUtils.priorityMergeFailureStrategies(
        stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    actionMap = FailureStrategiesUtils.priorityMergeFailureStrategies(
        stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    for (Map.Entry<FailureStrategyActionConfig, Collection<FailureType>> entry : actionMap.entrySet()) {
      FailureStrategyActionConfig action = entry.getKey();
      Set<FailureType> failureTypes = new HashSet<>(entry.getValue());
      NGFailureActionType actionType = action.getType();

      String nextNodeUuid = null;
      YamlField siblingField = obtainNextSiblingField(currentField);
      // Check if step is in parallel section then dont have nextNodeUUid set.
      if (siblingField != null && !checkIfStepIsInParallelSection(currentField)) {
        nextNodeUuid = siblingField.getNode().getUuid();
      }

      AdviserObtainment.Builder adviserObtainmentBuilder = AdviserObtainment.newBuilder();
      if (isStepInsideRollback) {
        if (actionType == NGFailureActionType.STAGE_ROLLBACK || actionType == NGFailureActionType.STEP_GROUP_ROLLBACK) {
          throw new InvalidRequestException("Step inside rollback section cannot have Rollback as failure strategy.");
        }
      }

      switch (actionType) {
        case IGNORE:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(IgnoreAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(IgnoreAdviserParameters.builder()
                                                                                .applicableFailureTypes(failureTypes)
                                                                                .nextNodeId(nextNodeUuid)
                                                                                .build())))
                  .build());
          break;
        case RETRY:
          RetryFailureActionConfig retryAction = (RetryFailureActionConfig) action;
          FailureStrategiesUtils.validateRetryFailureAction(retryAction);
          ParameterField<Integer> retryCount = retryAction.getSpecConfig().getRetryCount();
          FailureStrategyActionConfig actionUnderRetry = retryAction.getSpecConfig().getOnRetryFailure().getAction();
          adviserObtainmentList.add(getRetryAdviserObtainment(failureTypes, nextNodeUuid, adviserObtainmentBuilder,
              retryAction, retryCount, actionUnderRetry, currentField));
          break;
        case MARK_AS_SUCCESS:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(OnMarkSuccessAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(OnMarkSuccessAdviserParameters.builder()
                                                                                .applicableFailureTypes(failureTypes)
                                                                                .nextNodeId(nextNodeUuid)
                                                                                .build())))
                  .build());

          break;
        case ABORT:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(OnAbortAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                      OnAbortAdviserParameters.builder().applicableFailureTypes(failureTypes).build())))
                  .build());
          break;
        case STAGE_ROLLBACK:
          OnFailRollbackParameters rollbackParameters =
              getRollbackParameters(currentField, failureTypes, RollbackStrategy.STAGE_ROLLBACK);
          adviserObtainmentList.add(adviserObtainmentBuilder.setType(OnFailRollbackAdviser.ADVISER_TYPE)
                                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(rollbackParameters)))
                                        .build());
          break;
        case STEP_GROUP_ROLLBACK:
          OnFailRollbackParameters stepGroupRollbackParameters =
              getRollbackParameters(currentField, failureTypes, RollbackStrategy.STEP_GROUP_ROLLBACK);
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(OnFailRollbackAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(stepGroupRollbackParameters)))
                  .build());
          break;
        case MANUAL_INTERVENTION:
          ManualInterventionFailureActionConfig actionConfig = (ManualInterventionFailureActionConfig) action;
          FailureStrategiesUtils.validateManualInterventionFailureAction(actionConfig);
          FailureStrategyActionConfig actionUnderManualIntervention =
              actionConfig.getSpecConfig().getOnTimeout().getAction();
          adviserObtainmentList.add(getManualInterventionAdviserObtainment(
              failureTypes, adviserObtainmentBuilder, actionConfig, actionUnderManualIntervention, currentField));
          break;
        default:
          Switch.unhandled(actionType);
      }
    }

    /*
     * Adding OnSuccess adviser if step is inside rollback section else adding NextStep adviser for when condition to
     * work.
     */
    if (isStepInsideRollback) {
      AdviserObtainment onSuccessAdviserObtainment = getOnSuccessAdviserObtainment(currentField);
      if (onSuccessAdviserObtainment != null) {
        adviserObtainmentList.add(onSuccessAdviserObtainment);
      }
    } else {
      // Always add nextStep adviser at last, as its priority is less than, Do not change the order.
      AdviserObtainment nextStepAdviserObtainment = getNextStepAdviserObtainment(currentField);
      if (nextStepAdviserObtainment != null) {
        adviserObtainmentList.add(nextStepAdviserObtainment);
      }
    }

    return adviserObtainmentList;
  }

  protected AdviserObtainment getManualInterventionAdviserObtainment(Set<FailureType> failureTypes,
      AdviserObtainment.Builder adviserObtainmentBuilder, ManualInterventionFailureActionConfig actionConfig,
      FailureStrategyActionConfig actionUnderManualIntervention, YamlField currentField) {
    return adviserObtainmentBuilder.setType(ManualInterventionAdviser.ADVISER_TYPE)
        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
            ManualInterventionAdviserParameters.builder()
                .applicableFailureTypes(failureTypes)
                .timeoutAction(toRepairAction(actionUnderManualIntervention))
                .timeout((int) TimeoutUtils.getTimeoutInSeconds(actionConfig.getSpecConfig().getTimeout(), 0))
                .build())))
        .build();
  }

  protected AdviserObtainment getRetryAdviserObtainment(Set<FailureType> failureTypes, String nextNodeUuid,
      AdviserObtainment.Builder adviserObtainmentBuilder, RetryFailureActionConfig retryAction,
      ParameterField<Integer> retryCount, FailureStrategyActionConfig actionUnderRetry, YamlField currentField) {
    return adviserObtainmentBuilder.setType(RetryAdviser.ADVISER_TYPE)
        .setParameters(ByteString.copyFrom(
            kryoSerializer.asBytes(RetryAdviserParameters.builder()
                                       .applicableFailureTypes(failureTypes)
                                       .nextNodeId(nextNodeUuid)
                                       .repairActionCodeAfterRetry(toRepairAction(actionUnderRetry))
                                       .retryCount(retryCount.getValue())
                                       .waitIntervalList(retryAction.getSpecConfig()
                                                             .getRetryIntervals()
                                                             .getValue()
                                                             .stream()
                                                             .map(s -> (int) TimeoutUtils.getTimeoutInSeconds(s, 0))
                                                             .collect(Collectors.toList()))
                                       .build())))
        .build();
  }

  protected YamlField obtainNextSiblingField(YamlField currentField) {
    return currentField.getNode().nextSiblingFromParentArray(
        currentField.getName(), Arrays.asList(STEP, PARALLEL, STEP_GROUP));
  }

  private AdviserObtainment getOnSuccessAdviserObtainment(YamlField currentField) {
    if (currentField != null && currentField.getNode() != null) {
      if (checkIfStepIsInParallelSection(currentField)) {
        return null;
      }
      YamlField siblingField = obtainNextSiblingField(currentField);
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        return AdviserObtainment.newBuilder()
            .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
            .build();
      }
    }
    return null;
  }

  private AdviserObtainment getNextStepAdviserObtainment(YamlField currentField) {
    if (currentField != null && currentField.getNode() != null) {
      if (checkIfStepIsInParallelSection(currentField)) {
        return null;
      }
      YamlField siblingField = obtainNextSiblingField(currentField);
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        return AdviserObtainment.newBuilder()
            .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.NEXT_STEP.name()).build())
            .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                NextStepAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
            .build();
      }
    }
    return null;
  }

  private OnFailRollbackParameters getRollbackParameters(
      YamlField currentField, Set<FailureType> failureTypes, RollbackStrategy rollbackStrategy) {
    OnFailRollbackParametersBuilder rollbackParametersBuilder = OnFailRollbackParameters.builder();
    rollbackParametersBuilder.applicableFailureTypes(failureTypes);
    rollbackParametersBuilder.strategy(rollbackStrategy);
    rollbackParametersBuilder.strategyToUuid(getRollbackStrategyMap(currentField));
    return rollbackParametersBuilder.build();
  }

  protected Map<RollbackStrategy, String> getRollbackStrategyMap(YamlField currentField) {
    String stageNodeId = getStageNodeId(currentField);
    Map<RollbackStrategy, String> rollbackStrategyStringMap = new HashMap<>();
    rollbackStrategyStringMap.put(
        RollbackStrategy.STAGE_ROLLBACK, stageNodeId + OrchestrationConstants.COMBINED_ROLLBACK_ID_SUFFIX);
    rollbackStrategyStringMap.put(RollbackStrategy.STEP_GROUP_ROLLBACK, getStepGroupRollbackStepsNodeId(currentField));
    return rollbackStrategyStringMap;
  }

  private String getStepGroupRollbackStepsNodeId(YamlField currentField) {
    YamlNode stepGroup = YamlUtils.findParentNode(currentField.getNode(), STEP_GROUP);
    return getRollbackStepsNodeId(stepGroup);
  }

  private String getStageNodeId(YamlField currentField) {
    YamlNode stageNode = YamlUtils.findParentNode(currentField.getNode(), STAGE);
    if (stageNode == null) {
      return null;
    }
    return stageNode.getUuid();
  }

  private String getRollbackStepsNodeId(YamlNode currentNode) {
    YamlField rollbackSteps = null;
    if (currentNode != null) {
      rollbackSteps = currentNode.getField(ROLLBACK_STEPS);
    }
    String uuid = null;
    if (rollbackSteps != null) {
      uuid = rollbackSteps.getNode().getUuid();
    }
    return uuid;
  }

  protected RepairActionCode toRepairAction(FailureStrategyActionConfig action) {
    switch (action.getType()) {
      case IGNORE:
        return RepairActionCode.IGNORE;
      case MARK_AS_SUCCESS:
        return RepairActionCode.MARK_AS_SUCCESS;
      case ABORT:
        return RepairActionCode.END_EXECUTION;
      case STAGE_ROLLBACK:
        return RepairActionCode.STAGE_ROLLBACK;
      case STEP_GROUP_ROLLBACK:
        return RepairActionCode.STEP_GROUP_ROLLBACK;
      case MANUAL_INTERVENTION:
        return RepairActionCode.MANUAL_INTERVENTION;
      case RETRY:
        return RepairActionCode.RETRY;
      default:
        throw new InvalidRequestException(

            action.toString() + " Failure action doesn't have corresponding RepairAction Code.");
    }
  }

  private List<FailureStrategyConfig> getFieldFailureStrategies(
      YamlField currentField, String fieldName, boolean isStepInsideRollback) {
    YamlNode fieldNode = YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), fieldName);
    if (isStepInsideRollback && fieldNode != null) {
      // Check if found fieldNode is within rollbackSteps section
      YamlNode rollbackNode = YamlUtils.findParentNode(fieldNode, ROLLBACK_STEPS);
      if (rollbackNode == null) {
        return Collections.emptyList();
      }
    }
    if (fieldNode != null) {
      return getFailureStrategies(fieldNode);
    }
    return Collections.emptyList();
  }

  private List<FailureStrategyConfig> getFailureStrategies(YamlNode node) {
    YamlField failureStrategy = node.getField(FAILURE_STRATEGIES);
    List<FailureStrategyConfig> failureStrategyConfigs = null;

    try {
      if (failureStrategy != null) {
        failureStrategyConfigs =
            YamlUtils.read(failureStrategy.getNode().toString(), new TypeReference<List<FailureStrategyConfig>>() {});
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
    return failureStrategyConfigs;
  }

  // This is required as step can be inside stepGroup which can have Parallel and stepGroup itself can
  // be inside Parallel section.
  private boolean checkIfStepIsInParallelSection(YamlField currentField) {
    if (currentField != null && currentField.getNode() != null) {
      if (currentField.checkIfParentIsParallel(STEPS) || currentField.checkIfParentIsParallel(ROLLBACK_STEPS)) {
        // Check if step is inside StepGroup and StepGroup is inside Parallel but not the step.
        return YamlUtils.findParentNode(currentField.getNode(), STEP_GROUP) == null
            || currentField.checkIfParentIsParallel(STEP_GROUP);
      }
    }
    return false;
  }
}

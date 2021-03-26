package io.harness.plancreator.steps;

import static io.harness.pms.yaml.YAMLFieldNameConstants.EXECUTION;
import static io.harness.pms.yaml.YAMLFieldNameConstants.FAILURE_STRATEGIES;
import static io.harness.pms.yaml.YAMLFieldNameConstants.PARALLEL;
import static io.harness.pms.yaml.YAMLFieldNameConstants.ROLLBACK_STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP_GROUP;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.govern.Switch;
import io.harness.plancreator.beans.PlanCreationConstants;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.utils.RunInfoUtils;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.plan.creation.PlanCreatorUtils;
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
import io.harness.pms.sdk.core.adviser.rollback.RollbackNodeType;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.RollbackInfo.RollbackInfoBuilder;
import io.harness.pms.sdk.core.steps.io.RollbackStrategy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.WithRollbackInfo;
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
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureTypeConstants;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualFailureSpecConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureSpecConfig;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GenericStepPMSPlanCreator implements PartialPlanCreator<StepElementConfig> {
  private static final String DEFAULT_TIMEOUT = "10m";
  @Inject private KryoSerializer kryoSerializer;

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
    RollbackInfoBuilder rollbackInfoBuilder = RollbackInfo.builder();
    List<AdviserObtainment> adviserObtainmentFromMetaData =
        getAdviserObtainmentFromMetaData(ctx.getCurrentField(), rollbackInfoBuilder);

    if (stepElement.getTimeout() != null && stepElement.getTimeout().isExpression()) {
      throw new InvalidRequestException("Timeout field must be resolved in step: " + stepElement.getIdentifier());
    }

    if (stepElement.getStepSpecType() instanceof WithRollbackInfo) {
      if (((WithRollbackInfo) stepElement.getStepSpecType()).validateStageFailureStrategy()) {
        // Failure strategy should be present.
        List<FailureStrategyConfig> stageFailureStrategies = getFieldFailureStrategies(ctx.getCurrentField(), STAGE);
        if (EmptyPredicate.isEmpty(stageFailureStrategies)) {
          throw new InvalidRequestException("There should be atleast one failure strategy configured at stage level.");
        }

        // checking stageFailureStrategies is having one strategy with error type as AnyOther and along with that no
        // error type is involved
        if (!containsOnlyAnyOtherError(stageFailureStrategies)) {
          throw new InvalidRequestException(
              "Failure strategy should contain one error type as Anyother or it is having more than one error type along with Anyother.");
        }
      }

      String timeout = DEFAULT_TIMEOUT;
      if (stepElement.getTimeout() != null && stepElement.getTimeout().getValue() != null) {
        timeout = stepElement.getTimeout().getValue().getTimeoutString();
      }
      RollbackInfo rollbackInfo = rollbackInfoBuilder.build();
      BaseStepParameterInfo baseStepParameterInfo =
          BaseStepParameterInfo.builder()
              .timeout(ParameterField.createValueField(timeout))
              .rollbackInfo(rollbackInfo.getStrategy() != null ? rollbackInfo : null)
              .description(stepElement.getDescription())
              .skipCondition(stepElement.getSkipCondition())
              .name(stepElement.getName())
              .identifier(stepElement.getIdentifier())
              .build();
      stepParameters =
          ((WithRollbackInfo) stepElement.getStepSpecType()).getStepParametersWithRollbackInfo(baseStepParameterInfo);
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
            .whenCondition(RunInfoUtils.getRunCondition(stepElement.getWhen()))
            .timeoutObtainment(
                TimeoutObtainment.newBuilder()
                    .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
                    .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                        AbsoluteTimeoutParameters.builder().timeoutMillis(getTimeoutInMillis(stepElement)).build())))
                    .build())
            .build();
    return PlanCreationResponse.builder().node(stepPlanNode.getUuid(), stepPlanNode).build();
  }

  public boolean containsOnlyAnyOtherError(List<FailureStrategyConfig> stageFailureStrategies) {
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

  protected List<AdviserObtainment> getAdviserObtainmentFromMetaData(
      YamlField currentField, RollbackInfoBuilder rollbackInfoBuilder) {
    List<AdviserObtainment> adviserObtainmentList = new ArrayList<>();
    AdviserObtainment onSuccessAdviserObtainment = getOnSuccessAdviserObtainment(currentField);
    if (onSuccessAdviserObtainment != null) {
      adviserObtainmentList.add(onSuccessAdviserObtainment);
    }

    List<FailureStrategyConfig> stageFailureStrategies = getFieldFailureStrategies(currentField, STAGE);
    List<FailureStrategyConfig> stepGroupFailureStrategies = getFieldFailureStrategies(currentField, STEP_GROUP);
    List<FailureStrategyConfig> stepFailureStrategies = getFailureStrategies(currentField.getNode());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMap =
        FailureStrategiesUtils.priorityMergeFailureStrategies(
            stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    if (YamlUtils.findParentNode(currentField.getNode(), ROLLBACK_STEPS) != null) {
      actionMap = FailureStrategiesUtils.priorityMergeFailureStrategies(
          stepFailureStrategies, stepGroupFailureStrategies, null);
    }

    for (Map.Entry<FailureStrategyActionConfig, Collection<FailureType>> entry : actionMap.entrySet()) {
      FailureStrategyActionConfig action = entry.getKey();
      Set<FailureType> failureTypes = new HashSet<>(entry.getValue());
      NGFailureActionType actionType = action.getType();

      String nextNodeUuid = null;
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
          currentField.getName(), Arrays.asList(STEP, PARALLEL, STEP_GROUP));
      // Check if step is in parallel section then dont have nextNodeUUid set.
      if (siblingField != null && !checkIfStepIsInParallelSection(currentField)) {
        nextNodeUuid = siblingField.getNode().getUuid();
      }

      AdviserObtainment.Builder adviserObtainmentBuilder = AdviserObtainment.newBuilder();
      if (rollbackInfoBuilder != null) {
        getBasicRollbackInfo(currentField, failureTypes, rollbackInfoBuilder);
      }
      if (YamlUtils.findParentNode(currentField.getNode(), ROLLBACK_STEPS) != null) {
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
          ParameterField<Integer> retryCount = retryAction.getSpecConfig().getRetryCount();
          if (retryCount.isExpression()) {
            throw new InvalidRequestException("RetryCount fixed value is not given.");
          }
          if (retryAction.getSpecConfig().getRetryIntervals().isExpression()) {
            throw new InvalidRequestException("RetryIntervals cannot be expression/runtime input. Please give values.");
          }

          FailureStrategyActionConfig actionUnderRetry = retryAction.getSpecConfig().getOnRetryFailure().getAction();

          if (!validateActionAfterRetryFailure(actionUnderRetry)) {
            throw new InvalidRequestException("Retry action cannot have post retry failure action as Retry");
          }
          // validating Retry -> Manual Intervention -> Retry
          if (actionUnderRetry.getType().equals(NGFailureActionType.MANUAL_INTERVENTION)) {
            if (validateRetryActionUnderManualAction(
                    ((ManualInterventionFailureActionConfig) actionUnderRetry).getSpecConfig())) {
              throw new InvalidRequestException(
                  "Retry Action cannot be applied under Manual Action which itself is in Retry Action");
            }
          }
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(RetryAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                      RetryAdviserParameters.builder()
                          .applicableFailureTypes(failureTypes)
                          .nextNodeId(nextNodeUuid)
                          .repairActionCodeAfterRetry(toRepairAction(actionUnderRetry, rollbackInfoBuilder))
                          .retryCount(retryCount.getValue())
                          .waitIntervalList(retryAction.getSpecConfig()
                                                .getRetryIntervals()
                                                .getValue()
                                                .stream()
                                                .map(s -> (int) TimeoutUtils.getTimeoutInSeconds(s, 0))
                                                .collect(Collectors.toList()))
                          .build())))
                  .build());

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
          if (rollbackInfoBuilder != null) {
            rollbackInfoBuilder.strategy(RollbackStrategy.STAGE_ROLLBACK);
          }
          break;
        case STEP_GROUP_ROLLBACK:
          if (rollbackInfoBuilder != null) {
            rollbackInfoBuilder.strategy(RollbackStrategy.STEP_GROUP_ROLLBACK);
          }
          break;
        case MANUAL_INTERVENTION:
          ManualInterventionFailureActionConfig actionConfig = (ManualInterventionFailureActionConfig) action;

          FailureStrategyActionConfig actionUnderManualIntervention =
              actionConfig.getSpecConfig().getOnTimeout().getAction();
          if (actionUnderManualIntervention.getType().equals(NGFailureActionType.MANUAL_INTERVENTION)) {
            throw new InvalidRequestException("Manual Action cannot be applied as PostTimeOut Action");
          }
          // validating Manual Intervention -> Retry -> Manual Intervention
          if (actionUnderManualIntervention.getType().equals(NGFailureActionType.RETRY)) {
            if (validateManualActionUnderRetryAction(
                    ((RetryFailureActionConfig) actionUnderManualIntervention).getSpecConfig())) {
              throw new InvalidRequestException(
                  "Manual Action cannot be applied under Retry Action which itself is in Manual Action");
            }
          }
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(ManualInterventionAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                      ManualInterventionAdviserParameters.builder()
                          .applicableFailureTypes(failureTypes)
                          .timeoutAction(toRepairAction(actionUnderManualIntervention, rollbackInfoBuilder))
                          .timeout((int) TimeoutUtils.getTimeoutInSeconds(actionConfig.getSpecConfig().getTimeout(), 0))
                          .build())))
                  .build());
          break;
        default:
          Switch.unhandled(actionType);
      }
    }

    return adviserObtainmentList;
  }

  public boolean validateManualActionUnderRetryAction(RetryFailureSpecConfig retrySpecConfig) {
    return retrySpecConfig.getOnRetryFailure().getAction().getType().equals(NGFailureActionType.MANUAL_INTERVENTION);
  }

  public boolean validateRetryActionUnderManualAction(ManualFailureSpecConfig manualSpecConfig) {
    return manualSpecConfig.getOnTimeout().getAction().getType().equals(NGFailureActionType.RETRY);
  }

  public boolean validateActionAfterRetryFailure(FailureStrategyActionConfig action) {
    return action.getType() != NGFailureActionType.RETRY;
  }

  private AdviserObtainment getOnSuccessAdviserObtainment(YamlField currentField) {
    if (currentField != null && currentField.getNode() != null) {
      if (checkIfStepIsInParallelSection(currentField)) {
        return null;
      }
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
          currentField.getName(), Arrays.asList(STEP, PARALLEL, STEP_GROUP));
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

  private void getBasicRollbackInfo(
      YamlField currentField, Set<FailureType> failureTypes, RollbackInfoBuilder rollbackInfoBuilder) {
    rollbackInfoBuilder.failureTypes(failureTypes);
    rollbackInfoBuilder.identifier(currentField.getNode().getIdentifier());
    rollbackInfoBuilder.group(RollbackNodeType.STEP.name());
    String rollbackStepsNodeId = getStageRollbackStepsNodeId(currentField);
    String executionStepsNodeId = getExecutionStepsNodeId(currentField);
    rollbackInfoBuilder.nodeTypeToUuid(RollbackNodeType.STAGE.name(),
        rollbackStepsNodeId == null ? null : rollbackStepsNodeId + PlanCreationConstants.ROLLBACK_STEPS_NODE_ID_PREFIX);

    // Check if stepGroupsRollback is there or not.
    YamlNode executionField = YamlUtils.findParentNode(currentField.getNode(), EXECUTION);
    if (PlanCreatorUtils.checkIfAnyStepGroupRollback(executionField)) {
      rollbackInfoBuilder.nodeTypeToUuid(RollbackNodeType.STEP_GROUP_COMBINED.name(),
          executionStepsNodeId == null
              ? null
              : executionStepsNodeId + PlanCreationConstants.STEP_GROUPS_ROLLBACK_NODE_ID_PREFIX);
    } else {
      rollbackInfoBuilder.nodeTypeToUuid(RollbackNodeType.STEP_GROUP_COMBINED.name(), null);
    }

    rollbackInfoBuilder.nodeTypeToUuid(
        RollbackNodeType.STEP_GROUP.name(), getStepGroupRollbackStepsNodeId(currentField));
    if (PlanCreatorUtils.checkIfStageRollbackStepsPresent(executionField)) {
      rollbackInfoBuilder.nodeTypeToUuid(RollbackNodeType.BOTH_STEP_GROUP_STAGE.name(),
          executionStepsNodeId == null ? null : executionStepsNodeId + "_combinedRollback");
    } else {
      rollbackInfoBuilder.nodeTypeToUuid(RollbackNodeType.BOTH_STEP_GROUP_STAGE.name(), null);
    }
  }

  private String getStepGroupRollbackStepsNodeId(YamlField currentField) {
    YamlNode stepGroup = YamlUtils.findParentNode(currentField.getNode(), STEP_GROUP);
    return getRollbackStepsNodeId(stepGroup);
  }

  private String getStageRollbackStepsNodeId(YamlField currentField) {
    YamlNode execution = YamlUtils.findParentNode(currentField.getNode(), EXECUTION);
    return getRollbackStepsNodeId(execution);
  }

  private String getExecutionStepsNodeId(YamlField currentField) {
    YamlNode execution = YamlUtils.findParentNode(currentField.getNode(), EXECUTION);
    return execution == null
        ? null
        : Objects.requireNonNull(Objects.requireNonNull(execution).getField(STEPS)).getNode().getUuid();
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

  private RepairActionCode toRepairAction(FailureStrategyActionConfig action, RollbackInfoBuilder rollbackInfoBuilder) {
    switch (action.getType()) {
      case IGNORE:
        return RepairActionCode.IGNORE;
      case MARK_AS_SUCCESS:
        return RepairActionCode.MARK_AS_SUCCESS;
      case ABORT:
        return RepairActionCode.END_EXECUTION;
      case STAGE_ROLLBACK:
        if (rollbackInfoBuilder != null) {
          rollbackInfoBuilder.strategy(RollbackStrategy.STAGE_ROLLBACK);
        }
        return RepairActionCode.ON_FAIL;
      case STEP_GROUP_ROLLBACK:
        if (rollbackInfoBuilder != null) {
          rollbackInfoBuilder.strategy(RollbackStrategy.STEP_GROUP_ROLLBACK);
        }
        return RepairActionCode.ON_FAIL;
      case MANUAL_INTERVENTION:
        return RepairActionCode.MANUAL_INTERVENTION;
      case RETRY:
        return RepairActionCode.RETRY;
      default:
        throw new InvalidRequestException(

            action.toString() + " Failure action doesn't have corresponding RepairAction Code.");
    }
  }

  private List<FailureStrategyConfig> getFieldFailureStrategies(YamlField currentField, String fieldName) {
    YamlNode stage = YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), fieldName);
    if (stage != null) {
      return getFailureStrategies(stage);
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

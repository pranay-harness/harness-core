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
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviser;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters;
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
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.timeout.Timeout;
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
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GenericStepPMSPlanCreator implements PartialPlanCreator<StepElementConfig> {
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
    String nodeName;

    if (EmptyPredicate.isEmpty(stepElement.getName())) {
      nodeName = stepElement.getIdentifier();
    } else {
      nodeName = stepElement.getName();
    }

    PlanNode stepPlanNode =
        PlanNode.builder()
            .uuid(ctx.getCurrentField().getNode().getUuid())
            .name(nodeName)
            .identifier(stepElement.getIdentifier())
            .stepType(stepElement.getStepSpecType().getStepType())
            .group(StepOutcomeGroup.STEP.name())
            .stepParameters(stepElement.getStepSpecType().getStepParameters())
            .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder()
                                                    .setType(stepElement.getStepSpecType().getFacilitatorType())
                                                    .build())
                                       .build())
            .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
            .skipCondition(SkipInfoUtils.getSkipCondition(stepElement.getSkipCondition()))
            .build();
    return PlanCreationResponse.builder().node(stepPlanNode.getUuid(), stepPlanNode).build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainmentList = new ArrayList<>();
    AdviserObtainment onSuccessAdviserObtainment = getOnSuccessAdviserObtainment(currentField);
    if (onSuccessAdviserObtainment != null) {
      adviserObtainmentList.add(onSuccessAdviserObtainment);
    }
    if (YamlUtils.findParentNode(currentField.getNode(), ROLLBACK_STEPS) != null) {
      return adviserObtainmentList;
    }
    List<FailureStrategyConfig> stageFailureStrategies = getFieldFailureStrategies(currentField, STAGE);
    List<FailureStrategyConfig> stepGroupFailureStrategies = getFieldFailureStrategies(currentField, STEP_GROUP);
    List<FailureStrategyConfig> stepFailureStrategies = getFailureStrategies(currentField.getNode());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMap =
        FailureStrategiesUtils.priorityMergeFailureStrategies(
            stepFailureStrategies, stepGroupFailureStrategies, stageFailureStrategies);

    for (Map.Entry<FailureStrategyActionConfig, Collection<FailureType>> entry : actionMap.entrySet()) {
      FailureStrategyActionConfig action = entry.getKey();
      Set<FailureType> failureTypes = new HashSet<>(entry.getValue());
      NGFailureActionType actionType = action.getType();

      String nextNodeUuid = null;
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
          currentField.getName(), Arrays.asList(STEP, PARALLEL, STEP_GROUP));
      if (siblingField != null) {
        nextNodeUuid = siblingField.getNode().getUuid();
      }

      AdviserObtainment.Builder adviserObtainmentBuilder = AdviserObtainment.newBuilder();
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
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(RetryAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                      RetryAdviserParameters.builder()
                          .applicableFailureTypes(failureTypes)
                          .nextNodeId(nextNodeUuid)
                          .repairActionCodeAfterRetry(
                              toRepairAction(retryAction.getSpecConfig().getOnRetryFailure().getAction()))
                          .retryCount(retryAction.getSpecConfig().getRetryCount())
                          .waitIntervalList(
                              retryAction.getSpecConfig()
                                  .getRetryInterval()
                                  .stream()
                                  .map(s -> (int) TimeoutUtils.getTimeoutInSeconds(Timeout.fromString(s), 0))
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
          adviserObtainmentList.add(adviserObtainmentBuilder.setType(OnFailAdviser.ADVISER_TYPE)
                                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                            OnFailAdviserParameters.builder()
                                                .applicableFailureTypes(failureTypes)
                                                .nextNodeId(getStageRollbackStepsNodeId(currentField))
                                                .build())))
                                        .build());
          break;
        case STEP_GROUP_ROLLBACK:
          adviserObtainmentList.add(adviserObtainmentBuilder.setType(OnFailAdviser.ADVISER_TYPE)
                                        .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                                            OnFailAdviserParameters.builder()
                                                .applicableFailureTypes(failureTypes)
                                                .nextNodeId(getStepGroupRollbackStepsNodeId(currentField))
                                                .build())))
                                        .build());

          break;
        case MANUAL_INTERVENTION:
          adviserObtainmentList.add(
              adviserObtainmentBuilder.setType(ManualInterventionAdviser.ADVISER_TYPE)
                  .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                      ManualInterventionAdviserParameters.builder().applicableFailureTypes(failureTypes).build())))
                  .build());
          break;
        default:
          Switch.unhandled(actionType);
      }
    }

    return adviserObtainmentList;
  }

  private AdviserObtainment getOnSuccessAdviserObtainment(YamlField currentField) {
    if (currentField != null && currentField.getNode() != null) {
      if (currentField.checkIfParentIsParallel(STEPS) || currentField.checkIfParentIsParallel(ROLLBACK_STEPS)) {
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

  private String getStepGroupRollbackStepsNodeId(YamlField currentField) {
    YamlNode stepGroup = YamlUtils.findParentNode(currentField.getNode(), STEP_GROUP);

    return getRollbackStepsNodeId(stepGroup);
  }

  private String getStageRollbackStepsNodeId(YamlField currentField) {
    YamlNode execution = YamlUtils.findParentNode(currentField.getNode(), EXECUTION);
    return getRollbackStepsNodeId(execution);
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

  private RepairActionCode toRepairAction(FailureStrategyActionConfig action) {
    switch (action.getType()) {
      case IGNORE:
        return RepairActionCode.IGNORE;
      case MARK_AS_SUCCESS:
        return RepairActionCode.MARK_AS_SUCCESS;
      case ABORT:
        return RepairActionCode.END_EXECUTION;
      case STAGE_ROLLBACK:
      case STEP_GROUP_ROLLBACK:
        return RepairActionCode.ON_FAIL;
      case MANUAL_INTERVENTION:
        return RepairActionCode.MANUAL_INTERVENTION;
      default:
        throw new InvalidRequestException("Invalid yaml");
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
}

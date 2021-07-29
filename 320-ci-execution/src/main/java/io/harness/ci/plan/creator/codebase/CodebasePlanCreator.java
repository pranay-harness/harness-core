package io.harness.ci.plan.creator.codebase;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.ExecutionSource;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.yaml.YamlField;
import io.harness.serializer.KryoSerializer;
import io.harness.states.codebase.CodeBaseStep;
import io.harness.states.codebase.CodeBaseStepParameters;
import io.harness.states.codebase.CodeBaseTaskStep;
import io.harness.states.codebase.CodeBaseTaskStepParameters;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CodebasePlanCreator {
  public List<PlanNode> createPlanForCodeBase(
      PlanCreationContext ctx, YamlField ciCodeBaseField, String childNodeId, KryoSerializer kryoSerializer) {
    PlanCreationContextValue planCreationContextValue = ctx.getGlobalContext().get("metadata");
    ExecutionMetadata executionMetadata = planCreationContextValue.getMetadata();

    ExecutionTriggerInfo triggerInfo = executionMetadata.getTriggerInfo();
    TriggerPayload triggerPayload = planCreationContextValue.getTriggerPayload();

    CodeBase ciCodeBase = IntegrationStageUtils.getCiCodeBase(ciCodeBaseField.getNode());
    ExecutionSource executionSource =
        IntegrationStageUtils.buildExecutionSource(triggerInfo, triggerPayload, "codebase", ciCodeBase.getBuild());

    List<PlanNode> planNodeList = new ArrayList<>();
    PlanNode codeBaseDelegateTask = createPlanForCodeBaseTask(
        ciCodeBase, executionSource, OrchestrationFacilitatorType.TASK, ciCodeBaseField.getNode().getUuid());
    planNodeList.add(codeBaseDelegateTask);
    PlanNode codeBaseSyncTask = createPlanForCodeBaseTask(
        ciCodeBase, executionSource, OrchestrationFacilitatorType.SYNC, ciCodeBaseField.getNode().getUuid());
    planNodeList.add(codeBaseSyncTask);

    planNodeList.add(
        PlanNode.builder()
            .uuid(ciCodeBaseField.getNode().getUuid())
            .stepType(CodeBaseStep.STEP_TYPE)
            .name("codebase_node")
            .identifier("codebase_node")
            .stepParameters(CodeBaseStepParameters.builder()
                                .codeBaseSyncTaskId(codeBaseSyncTask.getUuid())
                                .codeBaseDelegateTaskId(codeBaseDelegateTask.getUuid())
                                .connectorRef(ciCodeBase.getConnectorRef())
                                .executionSource(executionSource)
                                .build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .adviserObtainment(
                AdviserObtainment.newBuilder()
                    .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                    .setParameters(ByteString.copyFrom(
                        kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(childNodeId).build())))
                    .build())
            .skipGraphType(SkipType.SKIP_NODE)
            .build());

    return planNodeList;
  }

  public PlanNode createPlanForCodeBaseTask(
      CodeBase ciCodeBase, ExecutionSource executionSource, String facilitatorType, String codeBaseId) {
    CodeBaseTaskStepParameters codeBaseTaskStepParameters = CodeBaseTaskStepParameters.builder()
                                                                .connectorRef(ciCodeBase.getConnectorRef())
                                                                .repoUrl(ciCodeBase.getRepoName())
                                                                .executionSource(executionSource)
                                                                .build();

    return PlanNode.builder()
        .uuid(codeBaseId + "-" + facilitatorType.toLowerCase())
        .stepType(CodeBaseTaskStep.STEP_TYPE)
        .identifier("codebase"
            + "-" + facilitatorType.toLowerCase())
        .stepParameters(codeBaseTaskStepParameters)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                   .setType(FacilitatorType.newBuilder().setType(facilitatorType).build())
                                   .build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }
}

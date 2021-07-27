package io.harness.ci.plan.creator.codebase;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.ManualExecutionSource;
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
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.protobuf.ByteString;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
@UtilityClass
public class CodebasePlanCreator {
  public PlanNode createPlanForCodeBase(
      PlanCreationContext ctx, YamlField ciCodeBaseField, String childNodeId, KryoSerializer kryoSerializer) {
    PlanCreationContextValue planCreationContextValue = ctx.getGlobalContext().get("metadata");
    ExecutionMetadata executionMetadata = planCreationContextValue.getMetadata();

    ExecutionTriggerInfo triggerInfo = executionMetadata.getTriggerInfo();
    TriggerPayload triggerPayload = planCreationContextValue.getTriggerPayload();

    CodeBase ciCodeBase = IntegrationStageUtils.getCiCodeBase(ciCodeBaseField.getNode());
    ExecutionSource executionSource =
        IntegrationStageUtils.buildExecutionSource(triggerInfo, triggerPayload, "codebase", ciCodeBase.getBuild());

    if (executionSource.getType() == ExecutionSource.Type.MANUAL) {
      if (isNotEmpty(((ManualExecutionSource) executionSource).getPrNumber())) {
        return PlanNode.builder()
            .uuid(ciCodeBaseField.getNode().getUuid())
            .stepType(CodeBaseStep.STEP_TYPE)
            .name("codebase_node")
            .identifier("codebase_node")
            .stepParameters(CodeBaseStepParameters.builder()
                                .connectorRef(ciCodeBase.getConnectorRef())
                                .executionSource(executionSource)
                                .build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                    .build())
            .adviserObtainment(
                AdviserObtainment.newBuilder()
                    .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                    .setParameters(ByteString.copyFrom(
                        kryoSerializer.asBytes(OnSuccessAdviserParameters.builder().nextNodeId(childNodeId).build())))
                    .build())
            .skipGraphType(SkipType.SKIP_NODE)
            .build();
      }
    }
    return null;
  }
}

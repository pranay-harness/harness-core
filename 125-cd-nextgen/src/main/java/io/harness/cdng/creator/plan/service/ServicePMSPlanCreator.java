package io.harness.cdng.creator.plan.service;

import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.sdk.core.facilitator.chain.TaskChainV3Facilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.serializer.KryoSerializer;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;

public class ServicePMSPlanCreator {
  public static PlanNode createPlanForServiceNode(
      YamlNode serviceNode, ServiceConfig serviceConfig, KryoSerializer kryoSerializer) {
    if (!serviceConfig.getName().isExpression() && EmptyPredicate.isEmpty(serviceConfig.getName().getValue())) {
      serviceConfig.setName(serviceConfig.getIdentifier());
    }
    ServiceConfig serviceOverrides = null;
    if (serviceConfig.getUseFromStage() != null) {
      ServiceUseFromStage.Overrides overrides = serviceConfig.getUseFromStage().getOverrides();
      if (overrides != null) {
        serviceOverrides =
            ServiceConfig.builder().name(overrides.getName()).description(overrides.getDescription()).build();
      }
    }
    StepParameters stepParameters =
        ServiceStepParameters.builder().service(serviceConfig).serviceOverrides(serviceOverrides).build();
    return PlanNode.builder()
        .uuid(serviceNode.getUuid())
        .stepType(ServiceStep.STEP_TYPE)
        .name(PlanCreatorConstants.SERVICE_NODE_NAME)
        .identifier(PlanCreatorConstants.SERVICE_NODE_IDENTIFIER)
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder().setType(TaskChainV3Facilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(serviceNode, kryoSerializer))
        .skipExpressionChain(false)
        .build();
  }

  private static List<AdviserObtainment> getAdviserObtainmentFromMetaData(
      YamlNode currentNode, KryoSerializer kryoSerializer) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentNode != null) {
      YamlField siblingField = currentNode.nextSiblingNodeFromParentObject("infrastructure");
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        adviserObtainments.add(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build());
      }
    }
    return adviserObtainments;
  }
}

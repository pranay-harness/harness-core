package io.harness.cdng.creator.plan.service;

import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.chain.TaskChainFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ServicePMSPlanCreator {
  public PlanNode createPlanForServiceNode(
      YamlField serviceField, ServiceConfig serviceConfig, KryoSerializer kryoSerializer) {
    YamlNode serviceNode = serviceField.getNode();
    ServiceConfig actualServiceConfig = getActualServiceConfig(serviceConfig, serviceField);
    if (!actualServiceConfig.getName().isExpression()
        && EmptyPredicate.isEmpty(actualServiceConfig.getName().getValue())) {
      actualServiceConfig.setName(actualServiceConfig.getIdentifier());
    }
    ServiceConfig serviceOverrides = null;
    if (actualServiceConfig.getUseFromStage() != null) {
      ServiceUseFromStage.Overrides overrides = actualServiceConfig.getUseFromStage().getOverrides();
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
            FacilitatorObtainment.newBuilder().setType(TaskChainFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(serviceNode, kryoSerializer))
        .skipExpressionChain(false)
        .build();
  }

  /** Method returns actual Service object by resolving useFromStage if present. */
  private ServiceConfig getActualServiceConfig(ServiceConfig serviceConfig, YamlField serviceField) {
    if (serviceConfig.getUseFromStage() != null) {
      if (serviceConfig.getServiceDefinition() != null) {
        throw new InvalidArgumentsException("KubernetesServiceSpec should not exist with UseFromStage.");
      }
      try {
        //  Add validation for not chaining of stages
        DeploymentStageConfig deploymentStage = YamlUtils.read(
            PlanCreatorUtils.getStageConfig(serviceField, serviceConfig.getUseFromStage().getStage().getValue())
                .getNode()
                .toString(),
            DeploymentStageConfig.class);
        if (deploymentStage != null) {
          return serviceConfig.applyUseFromStage(deploymentStage.getService());
        } else {
          throw new InvalidArgumentsException("Stage identifier given in useFromStage doesn't exist.");
        }
      } catch (IOException ex) {
        throw new InvalidRequestException("cannot convert stage YamlField to Stage Object");
      }
    }
    return serviceConfig;
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(
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

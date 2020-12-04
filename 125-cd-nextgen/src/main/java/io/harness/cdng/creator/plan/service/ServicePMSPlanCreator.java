package io.harness.cdng.creator.plan.service;

import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.StepParameters;

public class ServicePMSPlanCreator {
  public static PlanNode createPlanForServiceNode(String uuid, ServiceConfig serviceConfig) {
    String name = serviceConfig.getName() != null && serviceConfig.getName().getValue() != null
        ? serviceConfig.getName().getValue()
        : serviceConfig.getIdentifier().getValue();
    StepParameters stepParameters = ServiceStepParameters.builder().service(serviceConfig).build();
    return PlanNode.builder()
        .uuid(uuid)
        .stepType(ServiceStep.STEP_TYPE)
        .name(name)
        .identifier(serviceConfig.getIdentifier().getValue())
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder().setType(FacilitatorType.newBuilder().setType("CHILD").build()).build())
        .skipExpressionChain(false)
        .build();
  }
}

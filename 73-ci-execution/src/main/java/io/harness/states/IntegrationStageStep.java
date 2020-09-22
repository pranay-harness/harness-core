package io.harness.states;

import static io.harness.cdng.orchestration.StepUtils.createStepResponseFromChildResponse;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class IntegrationStageStep implements Step, ChildExecutable<IntegrationStageStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("INTEGRATION_STAGE_STEP").build();
  public static final String CHILD_PLAN_START_NODE = "io/harness/beans/execution";

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, IntegrationStageStepParameters integrationStageStepParameters, StepInputPackage inputPackage) {
    logger.info("Executing deployment stage with params [{}]", integrationStageStepParameters);
    // TODO Only K8 is supported currently
    if (integrationStageStepParameters.getIntegrationStage().getInfrastructure().getType().equals(
            "kubernetes-direct")) {
      K8sDirectInfraYaml k8sDirectInfraYaml =
          (K8sDirectInfraYaml) integrationStageStepParameters.getIntegrationStage().getInfrastructure();

      BuildNumber buildNumber = integrationStageStepParameters.getBuildNumber();
      String stageID = integrationStageStepParameters.getIntegrationStage().getIdentifier();

      K8PodDetails k8PodDetails = K8PodDetails.builder()
                                      .clusterName(k8sDirectInfraYaml.getSpec().getKubernetesCluster())
                                      .buildNumber(buildNumber)
                                      .stageID(stageID)
                                      .accountId(buildNumber.getAccountIdentifier())
                                      .namespace(k8sDirectInfraYaml.getSpec().getNamespace())
                                      .build();

      executionSweepingOutputResolver.consume(ambiance, ContextElement.podDetails, k8PodDetails, null);
    }
    final Map<String, String> fieldToExecutionNodeIdMap = integrationStageStepParameters.getFieldToExecutionNodeIdMap();

    final String executionNodeId = fieldToExecutionNodeIdMap.get(CHILD_PLAN_START_NODE);

    return ChildExecutableResponse.builder().childNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, IntegrationStageStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    logger.info("executed integration stage =[{}]", stepParameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}

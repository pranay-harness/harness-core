package io.harness.states;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntegrationStageStep implements ChildExecutable<IntegrationStageStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("INTEGRATION_STAGE_STEP").build();
  public static final String CHILD_PLAN_START_NODE = "io/harness/beans/execution";

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<IntegrationStageStepParameters> getStepParametersClass() {
    return IntegrationStageStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, IntegrationStageStepParameters integrationStageStepParameters, StepInputPackage inputPackage) {
    log.info("Executing deployment stage with params [{}]", integrationStageStepParameters);
    // TODO Only K8 is supported currently
    if (integrationStageStepParameters.getIntegrationStage().getInfrastructure().getType()
        == Infrastructure.Type.KUBERNETES_DIRECT) {
      K8sDirectInfraYaml k8sDirectInfraYaml =
          (K8sDirectInfraYaml) integrationStageStepParameters.getIntegrationStage().getInfrastructure();

      BuildNumberDetails buildNumberDetails = integrationStageStepParameters.getBuildNumberDetails();
      // TODO This is hack because identifier is null due to json ignore, we will solve it properly during PMS
      // Integration
      String stageID = integrationStageStepParameters.getIntegrationStageIdentifier();

      K8PodDetails k8PodDetails = K8PodDetails.builder()
                                      .clusterName(k8sDirectInfraYaml.getSpec().getConnectorRef())
                                      .buildNumberDetails(buildNumberDetails)
                                      .stageID(stageID)
                                      .accountId(buildNumberDetails.getAccountIdentifier())
                                      .namespace(k8sDirectInfraYaml.getSpec().getNamespace())
                                      .build();

      executionSweepingOutputResolver.consume(ambiance, ContextElement.podDetails, k8PodDetails, null);
    }
    final Map<String, String> fieldToExecutionNodeIdMap = integrationStageStepParameters.getFieldToExecutionNodeIdMap();

    final String executionNodeId = fieldToExecutionNodeIdMap.get(CHILD_PLAN_START_NODE);

    return ChildExecutableResponse.newBuilder().setChildNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, IntegrationStageStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("executed integration stage =[{}]", stepParameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}

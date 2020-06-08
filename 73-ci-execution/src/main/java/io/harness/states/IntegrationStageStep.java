package io.harness.states;

import static io.harness.cdng.orchestration.StepUtils.createStepResponseFromChildResponse;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Produces(Step.class)
@Slf4j
public class IntegrationStageStep implements Step, ChildExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("INTEGRATION_STAGE_STEP").build();
  public static final String CHILD_PLAN_START_NODE = "execution";

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    IntegrationStageStepParameters parameters = (IntegrationStageStepParameters) stepParameters;
    logger.info("Executing deployment stage with params [{}]", parameters);
    // TODO Only K8 is supported currently
    if (parameters.getIntegrationStage().getInfrastructure().getType().equals("kubernetes-direct")) {
      K8sDirectInfraYaml k8sDirectInfraYaml = (K8sDirectInfraYaml) parameters.getIntegrationStage().getInfrastructure();
      K8PodDetails k8PodDetails = K8PodDetails.builder()
                                      .clusterName(k8sDirectInfraYaml.getSpec().getK8sConnector())
                                      .namespace(k8sDirectInfraYaml.getSpec().getNamespace())
                                      .podName(parameters.getPodName())
                                      .build();
      executionSweepingOutputResolver.consume(ambiance, ContextElement.podDetails, k8PodDetails, null);
    }
    final Map<String, String> fieldToExecutionNodeIdMap = parameters.getFieldToExecutionNodeIdMap();

    final String executionNodeId = fieldToExecutionNodeIdMap.get(CHILD_PLAN_START_NODE);

    return ChildExecutableResponse.builder().childNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    final IntegrationStageStepParameters parameters = (IntegrationStageStepParameters) stepParameters;

    logger.info("executed integration stage =[{}]", parameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}

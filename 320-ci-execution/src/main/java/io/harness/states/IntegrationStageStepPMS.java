package io.harness.states;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.beans.VariablesOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tasks.ResponseData;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntegrationStageStepPMS implements ChildExecutable<IntegrationStageStepParametersPMS> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("IntegrationStageStepPMS").build();

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<IntegrationStageStepParametersPMS> getStepParametersClass() {
    return IntegrationStageStepParametersPMS.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(Ambiance ambiance,
      IntegrationStageStepParametersPMS integrationStageStepParametersPMS, StepInputPackage inputPackage) {
    log.info("Executing integration stage with params [{}]", integrationStageStepParametersPMS);

    Infrastructure infrastructure = integrationStageStepParametersPMS.getInfrastructure();

    if (infrastructure == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }
    if (infrastructure.getType() == Infrastructure.Type.KUBERNETES_DIRECT) {
      K8sDirectInfraYaml k8sDirectInfraYaml = (K8sDirectInfraYaml) infrastructure;

      int buildNumber = ambiance.getMetadata().getRunSequence();
      K8PodDetails k8PodDetails = K8PodDetails.builder()
                                      .clusterName(k8sDirectInfraYaml.getSpec().getConnectorRef())
                                      .stageID(integrationStageStepParametersPMS.getIdentifier())
                                      .accountId(AmbianceHelper.getAccountId(ambiance))
                                      .namespace(k8sDirectInfraYaml.getSpec().getNamespace())
                                      .build();

      executionSweepingOutputResolver.consume(
          ambiance, ContextElement.podDetails, k8PodDetails, StepOutcomeGroup.STAGE.name());
    }

    final String executionNodeId = integrationStageStepParametersPMS.getChildNodeID();
    return ChildExecutableResponse.newBuilder().setChildNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, IntegrationStageStepParametersPMS stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("executed integration stage =[{}]", stepParameters);

    StepResponse childResponse = createStepResponseFromChildResponse(responseDataMap);

    VariablesOutcome variablesOutcome = new VariablesOutcome();
    variablesOutcome.putAll(NGVariablesUtils.getMapOfVariables(
        stepParameters.getVariables(), Integer.parseInt(AmbianceHelper.getExpressionFunctorToken(ambiance))));
    return StepResponse.builder()
        .status(childResponse.getStatus())
        .failureInfo(childResponse.getFailureInfo())
        .stepOutcome(StepOutcome.builder().name(YAMLFieldNameConstants.VARIABLES).outcome(variablesOutcome).build())
        .build();
  }
}

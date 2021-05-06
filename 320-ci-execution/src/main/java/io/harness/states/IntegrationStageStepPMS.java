package io.harness.states;

import static io.harness.beans.steps.outcome.CIOutcomeNames.CI_STEP_ARTIFACT_OUTCOME;
import static io.harness.beans.steps.outcome.CIOutcomeNames.INTEGRATION_STAGE_OUTCOME;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.beans.steps.outcome.CIStepArtifactOutcome;
import io.harness.beans.steps.outcome.IntegrationStageOutcome;
import io.harness.beans.steps.outcome.IntegrationStageOutcome.IntegrationStageOutcomeBuilder;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.beans.VariablesSweepingOutput;
import io.harness.plancreator.steps.common.StageElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tasks.ResponseData;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class IntegrationStageStepPMS implements ChildExecutable<StageElementParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("IntegrationStageStepPMS").build();

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject OutcomeService outcomeService;

  @Override
  public Class<StageElementParameters> getStepParametersClass() {
    return StageElementParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StageElementParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Executing integration stage with params [{}]", stepParameters);

    IntegrationStageStepParametersPMS integrationStageStepParametersPMS =
        (IntegrationStageStepParametersPMS) stepParameters.getSpecConfig();

    Infrastructure infrastructure = integrationStageStepParametersPMS.getInfrastructure();

    if (infrastructure == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }
    K8PodDetails k8PodDetails = K8PodDetails.builder()
                                    .stageID(stepParameters.getIdentifier())
                                    .accountId(AmbianceHelper.getAccountId(ambiance))
                                    .build();

    executionSweepingOutputResolver.consume(
        ambiance, ContextElement.podDetails, k8PodDetails, StepOutcomeGroup.STAGE.name());
    VariablesSweepingOutput variablesSweepingOutput = getVariablesSweepingOutput(ambiance, stepParameters);
    executionSweepingOutputResolver.consume(
        ambiance, YAMLFieldNameConstants.VARIABLES, variablesSweepingOutput, StepOutcomeGroup.STAGE.name());

    final String executionNodeId = integrationStageStepParametersPMS.getChildNodeID();
    return ChildExecutableResponse.newBuilder().setChildNodeId(executionNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StageElementParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("executed integration stage =[{}]", stepParameters);
    IntegrationStageStepParametersPMS integrationStageStepParametersPMS =
        (IntegrationStageStepParametersPMS) stepParameters.getSpecConfig();
    StepResponseBuilder stepResponseBuilder = createStepResponseFromChildResponse(responseDataMap).toBuilder();
    List<String> stepIdentifiers = integrationStageStepParametersPMS.getStepIdentifiers();
    if (isNotEmpty(stepIdentifiers)) {
      List<Outcome> outcomes = stepIdentifiers.stream()
                                   .map(stepIdentifier
                                       -> outcomeService.resolveOptional(
                                           ambiance, RefObjectUtils.getOutcomeRefObject("artifact-" + stepIdentifier)))
                                   .filter(OptionalOutcome::isFound)
                                   .map(OptionalOutcome::getOutcome)
                                   .collect(Collectors.toList());

      if (isNotEmpty(outcomes)) {
        IntegrationStageOutcomeBuilder integrationStageOutcomeBuilder = IntegrationStageOutcome.builder();
        for (Outcome outcome : outcomes) {
          if (CI_STEP_ARTIFACT_OUTCOME.equals(outcome.getType())) {
            CIStepArtifactOutcome ciStepArtifactOutcome = (CIStepArtifactOutcome) outcome;

            if (ciStepArtifactOutcome.getStepArtifacts() != null) {
              if (isNotEmpty(ciStepArtifactOutcome.getStepArtifacts().getPublishedFileArtifacts())) {
                ciStepArtifactOutcome.getStepArtifacts().getPublishedFileArtifacts().forEach(
                    integrationStageOutcomeBuilder::fileArtifact);
              }
              if (isNotEmpty(ciStepArtifactOutcome.getStepArtifacts().getPublishedImageArtifacts())) {
                ciStepArtifactOutcome.getStepArtifacts().getPublishedImageArtifacts().forEach(
                    integrationStageOutcomeBuilder::imageArtifact);
              }
            }
          }
        }

        stepResponseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                            .name(INTEGRATION_STAGE_OUTCOME)
                                            .outcome(integrationStageOutcomeBuilder.build())
                                            .build());
      }
    }

    return stepResponseBuilder.build();
  }

  @NotNull
  private VariablesSweepingOutput getVariablesSweepingOutput(Ambiance ambiance, StageElementParameters stepParameters) {
    VariablesSweepingOutput variablesSweepingOutput = new VariablesSweepingOutput();
    variablesSweepingOutput.putAll(NGVariablesUtils.getMapOfVariables(
        stepParameters.getOriginalVariables(), ambiance.getExpressionFunctorToken()));
    return variablesSweepingOutput;
  }
}

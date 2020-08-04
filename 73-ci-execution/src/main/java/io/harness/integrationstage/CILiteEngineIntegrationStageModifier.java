package io.harness.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.intfc.StageType;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps for setting up pod and
 * adding cleanup
 */

@Slf4j
@Singleton
public class CILiteEngineIntegrationStageModifier implements StageExecutionModifier {
  @Inject private CILiteEngineStepGroupUtils ciLiteEngineStepGroupUtils;

  @Override
  public ExecutionElement modifyExecutionPlan(ExecutionElement execution, StageType stageType) {
    IntegrationStage integrationStage = (IntegrationStage) stageType;
    return getCILiteEngineTaskExecution(integrationStage);
  }

  private ExecutionElement getCILiteEngineTaskExecution(IntegrationStage integrationStage) {
    // TODO Only git is supported currently
    if (integrationStage.getGitConnector().getType().equals("git")) {
      GitConnectorYaml gitConnectorYaml = (GitConnectorYaml) integrationStage.getGitConnector();
      return ExecutionElement.builder()
          .steps(ciLiteEngineStepGroupUtils.createExecutionWrapperWithLiteEngineSteps(
              integrationStage, getBranchName(integrationStage), gitConnectorYaml.getIdentifier()))
          .build();
    } else {
      throw new IllegalArgumentException("Input connector type is not of type git");
    }
  }

  private String getBranchName(IntegrationStage integrationStage) {
    Optional<CIStepInfo> stepInfo =
        integrationStage.getExecution()
            .getSteps()
            .stream()
            .filter(executionWrapper -> executionWrapper instanceof StepElement)
            .filter(executionWrapper -> ((StepElement) executionWrapper).getStepSpecType() instanceof GitCloneStepInfo)
            .findFirst()
            .map(executionWrapper -> (GitCloneStepInfo) ((StepElement) executionWrapper).getStepSpecType());
    if (stepInfo.isPresent()) {
      GitCloneStepInfo gitCloneStepInfo = (GitCloneStepInfo) stepInfo.get();
      return gitCloneStepInfo.getBranch();
    } else {
      throw new InvalidRequestException("Failed to execute pipeline, Git clone section is missing");
    }
  }
}

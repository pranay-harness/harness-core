package io.harness.integrationstage;

import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment;
import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_LITE_ENGINE;
import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_MANAGER;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class CILiteEngineStepGroupUtils {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  private static final String BUILD_NUMBER = "buildnumber";
  private static final String YAML_RUN_STEP_STRING = "run";
  @Inject private LiteEngineTaskStepGenerator liteEngineTaskStepGenerator;
  private static final SecureRandom random = new SecureRandom();

  public List<ExecutionWrapper> createExecutionWrapperWithLiteEngineSteps(
      IntegrationStage integrationStage, String branchName, String gitConnectorIdentifier) {
    String buildNumber = BUILD_NUMBER + random.nextInt(100000); // TODO Have incremental build number

    List<ExecutionWrapper> mainEngineExecutionSections = new ArrayList<>();

    if (integrationStage.getExecution() == null || isEmpty(integrationStage.getExecution().getSteps())) {
      return mainEngineExecutionSections;
    }

    List<ExecutionWrapper> executionSections = integrationStage.getExecution().getSteps();

    boolean usePVC = containsManagerStep(executionSections);

    List<ExecutionWrapper> liteEngineExecutionSections = new ArrayList<>();
    int liteEngineCounter = 0;

    for (ExecutionWrapper executionWrapper : executionSections) {
      if (isLiteEngineStep(executionWrapper)) {
        liteEngineExecutionSections.add(executionWrapper);
      } else if (isCIManagerStep(executionWrapper)) {
        if (isNotEmpty(liteEngineExecutionSections)) {
          liteEngineCounter++;
          ExecutionWrapper liteEngineStepExecutionWrapper =
              fetchLiteEngineStepExecutionWrapper(liteEngineExecutionSections, liteEngineCounter, integrationStage,
                  branchName, gitConnectorIdentifier, buildNumber, usePVC);

          mainEngineExecutionSections.add(liteEngineStepExecutionWrapper);
          liteEngineExecutionSections = new ArrayList<>();
        }

        mainEngineExecutionSections.add(executionWrapper);
      }
    }

    if (isNotEmpty(liteEngineExecutionSections)) {
      liteEngineCounter++;
      ExecutionWrapper liteEngineStepExecutionWrapper = fetchLiteEngineStepExecutionWrapper(liteEngineExecutionSections,
          liteEngineCounter, integrationStage, branchName, gitConnectorIdentifier, buildNumber, usePVC);

      mainEngineExecutionSections.add(liteEngineStepExecutionWrapper);
    }

    return mainEngineExecutionSections;
  }

  private ExecutionWrapper fetchLiteEngineStepExecutionWrapper(List<ExecutionWrapper> liteEngineExecutionSections,
      Integer liteEngineCounter, IntegrationStage integrationStage, String branchName, String gitConnectorIdentifier,
      String buildNumber, boolean usePVC) {
    Integer parallelism = calculateParallelism(liteEngineExecutionSections);
    LiteEngineTaskStepInfo liteEngineTaskStepInfo = liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(
        ExecutionElement.builder().steps(liteEngineExecutionSections).build(), branchName, gitConnectorIdentifier,
        integrationStage, buildNumber, parallelism, liteEngineCounter, usePVC);

    return StepElement.builder()
        .identifier(LITE_ENGINE_TASK + liteEngineCounter)
        .stepSpecType(liteEngineTaskStepInfo)
        .build();
  }

  private boolean isLiteEngineStep(ExecutionWrapper executionWrapper) {
    return !isCIManagerStep(executionWrapper);
  }

  private boolean containsManagerStep(List<ExecutionWrapper> executionSections) {
    return executionSections.stream().anyMatch(this ::isCIManagerStep);
  }

  private boolean isCIManagerStep(ExecutionWrapper executionWrapper) {
    if (executionWrapper != null) {
      if (executionWrapper instanceof StepElement) {
        StepElement stepElement = (StepElement) executionWrapper;
        if (stepElement.getStepSpecType() instanceof CIStepInfo) {
          CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
          return ciStepInfo.getNonYamlInfo().getStepInfoType().getCiStepExecEnvironment() == CI_MANAGER;
        } else {
          throw new InvalidRequestException("Non CIStepInfo is not supported");
        }
      } else if (executionWrapper instanceof ParallelStepElement) {
        ParallelStepElement parallel = (ParallelStepElement) executionWrapper;
        CIStepExecEnvironment ciStepExecEnvironment = validateAndFetchParallelStepsType(parallel);
        return ciStepExecEnvironment == CI_MANAGER;
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
    return false;
  }

  private Integer calculateParallelism(List<ExecutionWrapper> executionSections) {
    int parallelism = 1;
    for (ExecutionWrapper executionWrapper : executionSections) {
      if (executionWrapper != null) {
        if (executionWrapper instanceof ParallelStepElement) {
          ParallelStepElement parallel = (ParallelStepElement) executionWrapper;
          CIStepExecEnvironment ciStepExecEnvironment = validateAndFetchParallelStepsType(parallel);
          if (ciStepExecEnvironment == CI_LITE_ENGINE) {
            int parallelRunStepsCount =
                (int) parallel.getSections()
                    .stream()
                    .filter(executionWrapperInParallel -> executionWrapperInParallel instanceof StepElement)
                    .map(executionWrapperInParallel -> (StepElement) executionWrapperInParallel)
                    .filter(stepElement -> stepElement.getType().equals(YAML_RUN_STEP_STRING))
                    .count();
            parallelism = Math.max(parallelism, parallelRunStepsCount);
          }
        }
      }
    }
    return parallelism;
  }

  private CIStepExecEnvironment validateAndFetchParallelStepsType(ParallelStepElement parallel) {
    CIStepExecEnvironment ciStepExecEnvironment = null;

    if (parallel.getSections() != null) {
      for (ExecutionWrapper executionWrapper : parallel.getSections()) {
        if (executionWrapper instanceof StepElement) {
          StepElement stepElement = (StepElement) executionWrapper;
          if (stepElement.getStepSpecType() instanceof CIStepInfo) {
            CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
            if (ciStepExecEnvironment == null
                || (ciStepExecEnvironment
                       == ciStepInfo.getNonYamlInfo().getStepInfoType().getCiStepExecEnvironment())) {
              ciStepExecEnvironment = ciStepInfo.getNonYamlInfo().getStepInfoType().getCiStepExecEnvironment();
            } else {
              throw new InvalidRequestException("All parallel steps can either run on manager or on lite engine");
            }
          } else {
            throw new InvalidRequestException("Non CIStepInfo is not supported");
          }
        }
      }
    }
    return ciStepExecEnvironment;
  }
}

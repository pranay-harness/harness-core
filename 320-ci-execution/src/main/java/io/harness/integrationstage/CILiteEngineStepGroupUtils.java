package io.harness.integrationstage;

import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment;
import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_MANAGER;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_DEPTH;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_DEPTH_ATTRIBUTE;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_IMAGE;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_MANUAL_DEPTH;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_STEP_ID;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_STEP_NAME;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CILiteEngineStepGroupUtils {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  private static final String BUILD_NUMBER = "buildnumber";
  @Inject private LiteEngineTaskStepGenerator liteEngineTaskStepGenerator;
  private static final SecureRandom random = new SecureRandom();

  public List<ExecutionWrapper> createExecutionWrapperWithLiteEngineSteps(
      IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs, CodeBase ciCodebase, String accountId) {
    String buildNumber = BUILD_NUMBER + random.nextInt(100000); // TODO Have incremental build number

    List<ExecutionWrapper> mainEngineExecutionSections = new ArrayList<>();

    if (integrationStage.getExecution() == null || isEmpty(integrationStage.getExecution().getSteps())) {
      return mainEngineExecutionSections;
    }

    List<ExecutionWrapper> executionSections = integrationStage.getExecution().getSteps();

    boolean usePVC = containsManagerStep(executionSections);

    log.info(
        "Creating CI execution wrapper step info with lite engine step for integration stage {} and build number {} with pvc {}",
        integrationStage.getIdentifier(), ciExecutionArgs.getBuildNumberDetails().getBuildNumber(), usePVC);

    List<ExecutionWrapper> liteEngineExecutionSections = new ArrayList<>();
    if (!integrationStage.isSkipGitClone()) {
      liteEngineExecutionSections.add(getGitCloneStep(ciExecutionArgs));
    }
    int liteEngineCounter = 0;
    for (ExecutionWrapper executionWrapper : executionSections) {
      if (isLiteEngineStep(executionWrapper)) {
        liteEngineExecutionSections.add(executionWrapper);
      } else if (isCIManagerStep(executionWrapper)) {
        if (isNotEmpty(liteEngineExecutionSections)) {
          liteEngineCounter++;
          ExecutionWrapper liteEngineStepExecutionWrapper =
              fetchLiteEngineStepExecutionWrapper(liteEngineExecutionSections, liteEngineCounter, integrationStage,
                  ciExecutionArgs, ciCodebase, buildNumber, usePVC, accountId);

          mainEngineExecutionSections.add(liteEngineStepExecutionWrapper);
          // Also execute each lite engine step individually on main engine
          mainEngineExecutionSections.addAll(liteEngineExecutionSections);

          liteEngineExecutionSections = new ArrayList<>();
        }

        mainEngineExecutionSections.add(executionWrapper);
      }
    }

    if (isNotEmpty(liteEngineExecutionSections)) {
      liteEngineCounter++;
      ExecutionWrapper liteEngineStepExecutionWrapper = fetchLiteEngineStepExecutionWrapper(liteEngineExecutionSections,
          liteEngineCounter, integrationStage, ciExecutionArgs, ciCodebase, buildNumber, usePVC, accountId);

      mainEngineExecutionSections.add(liteEngineStepExecutionWrapper);
      // Also execute each lite engine step individually on main engine
      mainEngineExecutionSections.addAll(liteEngineExecutionSections);
    }

    log.info("Creation execution section for BuildId {} with {} number of lite engine steps",
        ciExecutionArgs.getBuildNumberDetails().getBuildNumber(), liteEngineCounter);

    return mainEngineExecutionSections;
  }

  private ExecutionWrapper fetchLiteEngineStepExecutionWrapper(List<ExecutionWrapper> liteEngineExecutionSections,
      Integer liteEngineCounter, IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs,
      CodeBase ciCodebase, String buildNumber, boolean usePVC, String accountId) {
    LiteEngineTaskStepInfo liteEngineTaskStepInfo = liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(
        ExecutionElement.builder().steps(liteEngineExecutionSections).build(), ciCodebase, integrationStage,
        ciExecutionArgs, buildNumber, liteEngineCounter, usePVC, accountId);

    return StepElement.builder()
        .identifier(LITE_ENGINE_TASK + liteEngineCounter)
        .stepSpecType(liteEngineTaskStepInfo)
        .build();
  }

  private boolean isLiteEngineStep(ExecutionWrapper executionWrapper) {
    return !isCIManagerStep(executionWrapper);
  }

  private boolean containsManagerStep(List<ExecutionWrapper> executionSections) {
    return executionSections.stream().anyMatch(this::isCIManagerStep);
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

  private StepElement getGitCloneStep(CIExecutionArgs ciExecutionArgs) {
    Integer cloneDepth = GIT_CLONE_DEPTH;
    if (ciExecutionArgs.getExecutionSource().getType() == ExecutionSource.Type.MANUAL) {
      cloneDepth = GIT_CLONE_MANUAL_DEPTH;
    }

    Map<String, String> settings = new HashMap<>();
    settings.put(GIT_CLONE_DEPTH_ATTRIBUTE, cloneDepth.toString());
    PluginStepInfo step = PluginStepInfo.builder()
                              .identifier(GIT_CLONE_STEP_ID)
                              .image(GIT_CLONE_IMAGE)
                              .name(GIT_CLONE_STEP_NAME)
                              .settings(settings)
                              .build();
    return StepElement.builder()
        .identifier(GIT_CLONE_STEP_ID)
        .name(GIT_CLONE_STEP_NAME)
        .type(CIStepInfoType.PLUGIN.name().toLowerCase())
        .stepSpecType(step)
        .build();
  }
}

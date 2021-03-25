package io.harness.ci.integrationstage;

import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment;
import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_MANAGER;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_DEPTH;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_DEPTH_ATTRIBUTE;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_IMAGE;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_MANUAL_DEPTH;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_STEP_ID;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_STEP_NAME;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.utils.JsonPipelineUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
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
  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;

  public List<ExecutionWrapperConfig> createExecutionWrapperWithLiteEngineSteps(StageElementConfig stageElementConfig,
      CIExecutionArgs ciExecutionArgs, CodeBase ciCodebase, String podName, Infrastructure infrastructure) {
    List<ExecutionWrapperConfig> mainEngineExecutionSections = new ArrayList<>();

    IntegrationStageConfig integrationStageConfig = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);

    if (integrationStageConfig.getExecution() == null || hasNone(integrationStageConfig.getExecution().getSteps())) {
      return mainEngineExecutionSections;
    }

    List<ExecutionWrapperConfig> executionSections = integrationStageConfig.getExecution().getSteps();

    boolean usePVC = containsManagerStep(executionSections);

    log.info(
        "Creating CI execution wrapper step info with lite engine step for integration stage {} and build number {} with pvc {}",
        stageElementConfig.getIdentifier(), ciExecutionArgs.getBuildNumberDetails().getBuildNumber(), usePVC);

    List<ExecutionWrapperConfig> liteEngineExecutionSections = new ArrayList<>();
    boolean gitClone = RunTimeInputHandler.resolveGitClone(integrationStageConfig.getCloneCodebase());

    if (gitClone) {
      liteEngineExecutionSections.add(getGitCloneStep(ciExecutionArgs));
    }
    int liteEngineCounter = 0;
    for (ExecutionWrapperConfig executionWrapper : executionSections) {
      if (isLiteEngineStep(executionWrapper)) {
        liteEngineExecutionSections.add(executionWrapper);
      } else if (isCIManagerStep(executionWrapper)) {
        if (hasSome(liteEngineExecutionSections)) {
          liteEngineCounter++;
          ExecutionWrapperConfig liteEngineStepExecutionWrapper =
              fetchLiteEngineStepExecutionWrapper(liteEngineExecutionSections, liteEngineCounter, stageElementConfig,
                  ciExecutionArgs, ciCodebase, podName, usePVC, infrastructure);

          mainEngineExecutionSections.add(liteEngineStepExecutionWrapper);
          // Also execute each lite engine step individually on main engine
          mainEngineExecutionSections.addAll(liteEngineExecutionSections);

          liteEngineExecutionSections = new ArrayList<>();
        }

        mainEngineExecutionSections.add(executionWrapper);
      }
    }

    if (hasSome(liteEngineExecutionSections)) {
      liteEngineCounter++;
      ExecutionWrapperConfig liteEngineStepExecutionWrapper =
          fetchLiteEngineStepExecutionWrapper(liteEngineExecutionSections, liteEngineCounter, stageElementConfig,
              ciExecutionArgs, ciCodebase, podName, usePVC, infrastructure);

      mainEngineExecutionSections.add(liteEngineStepExecutionWrapper);
      // Also execute each lite engine step individually on main engine
      mainEngineExecutionSections.addAll(liteEngineExecutionSections);
    }

    log.info("Creation execution section for BuildId {} with {} number of lite engine steps",
        ciExecutionArgs.getBuildNumberDetails().getBuildNumber(), liteEngineCounter);

    return mainEngineExecutionSections;
  }

  private ExecutionWrapperConfig fetchLiteEngineStepExecutionWrapper(
      List<ExecutionWrapperConfig> liteEngineExecutionSections, Integer liteEngineCounter,
      StageElementConfig integrationStage, CIExecutionArgs ciExecutionArgs, CodeBase ciCodebase, String podName,
      boolean usePVC, Infrastructure infrastructure) {
    // TODO Do not generate new id
    LiteEngineTaskStepInfo liteEngineTaskStepInfo = liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(
        ExecutionElementConfig.builder().uuid(generateUuid()).steps(liteEngineExecutionSections).build(), ciCodebase,
        integrationStage, ciExecutionArgs, podName, liteEngineCounter, usePVC, infrastructure);

    try {
      String uuid = generateUuid();
      String jsonString = JsonPipelineUtils.writeJsonString(StepElementConfig.builder()
                                                                .identifier(LITE_ENGINE_TASK + liteEngineCounter)
                                                                .uuid(generateUuid())
                                                                .type("liteEngineTask")
                                                                .stepSpecType(liteEngineTaskStepInfo)
                                                                .build());
      JsonNode jsonNode = JsonPipelineUtils.getMapper().readTree(jsonString);
      return ExecutionWrapperConfig.builder().uuid(uuid).step(jsonNode).build();
    } catch (IOException e) {
      throw new CIStageExecutionException("Failed to create gitclone step", e);
    }
  }

  private boolean isLiteEngineStep(ExecutionWrapperConfig executionWrapper) {
    return !isCIManagerStep(executionWrapper);
  }

  private boolean containsManagerStep(List<ExecutionWrapperConfig> executionSections) {
    return executionSections.stream().anyMatch(this::isCIManagerStep);
  }

  private boolean isCIManagerStep(ExecutionWrapperConfig executionWrapperConfig) {
    if (executionWrapperConfig != null) {
      if (executionWrapperConfig.getStep() != null && !executionWrapperConfig.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapperConfig);
        if (stepElementConfig.getStepSpecType() instanceof CIStepInfo) {
          CIStepInfo ciStepInfo = (CIStepInfo) stepElementConfig.getStepSpecType();
          return ciStepInfo.getNonYamlInfo().getStepInfoType().getCiStepExecEnvironment() == CI_MANAGER;
        } else {
          throw new InvalidRequestException("Non CIStepInfo is not supported");
        }
      } else if (executionWrapperConfig.getParallel() != null && !executionWrapperConfig.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig =
            IntegrationStageUtils.getParallelStepElementConfig(executionWrapperConfig);

        CIStepExecEnvironment ciStepExecEnvironment = validateAndFetchParallelStepsType(parallelStepElementConfig);
        return ciStepExecEnvironment == CI_MANAGER;
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
    return false;
  }

  private CIStepExecEnvironment validateAndFetchParallelStepsType(ParallelStepElementConfig parallel) {
    CIStepExecEnvironment ciStepExecEnvironment = null;

    if (hasSome(parallel.getSections())) {
      for (ExecutionWrapperConfig executionWrapper : parallel.getSections()) {
        if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
          StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);

          if (stepElementConfig.getStepSpecType() instanceof CIStepInfo) {
            CIStepInfo ciStepInfo = (CIStepInfo) stepElementConfig.getStepSpecType();
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

  private ExecutionWrapperConfig getGitCloneStep(CIExecutionArgs ciExecutionArgs) {
    Integer cloneDepth = GIT_CLONE_DEPTH;
    if (ciExecutionArgs.getExecutionSource().getType() == ExecutionSource.Type.MANUAL) {
      cloneDepth = GIT_CLONE_MANUAL_DEPTH;
    }

    Map<String, String> settings = new HashMap<>();
    settings.put(GIT_CLONE_DEPTH_ATTRIBUTE, cloneDepth.toString());
    PluginStepInfo step =
        PluginStepInfo.builder()
            .identifier(GIT_CLONE_STEP_ID)
            .image(ParameterField.createValueField(GIT_CLONE_IMAGE))
            .connectorRef(ParameterField.createValueField(ciExecutionServiceConfig.getDefaultInternalImageConnector()))
            .name(GIT_CLONE_STEP_NAME)
            .settings(ParameterField.createValueField(settings))
            .build();

    String uuid = generateUuid();
    StepElementConfig stepElementConfig = StepElementConfig.builder()
                                              .identifier(GIT_CLONE_STEP_ID)
                                              .name(GIT_CLONE_STEP_NAME)
                                              .uuid(generateUuid())
                                              .type("Plugin")
                                              .stepSpecType(step)
                                              .build();

    try {
      String jsonString = JsonPipelineUtils.writeJsonString(stepElementConfig);
      JsonNode jsonNode = JsonPipelineUtils.getMapper().readTree(jsonString);
      return ExecutionWrapperConfig.builder().uuid(uuid).step(jsonNode).build();
    } catch (IOException e) {
      throw new CIStageExecutionException("Failed to create gitclone step", e);
    }
  }
}

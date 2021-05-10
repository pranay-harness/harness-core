package io.harness.ci.integrationstage;

import static io.harness.common.CIExecutionConstants.IMAGE_PATH_SPLIT_REGEX;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.CustomExecutionSource;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.k8s.model.ImageDetails;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.util.WebhookTriggerProcessorUtils;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.BuildType;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;

import java.io.IOException;
import java.util.Arrays;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class IntegrationStageUtils {
  public IntegrationStageConfig getIntegrationStageConfig(StageElementConfig stageElementConfig) {
    if (stageElementConfig.getType().equals("CI")) {
      return (IntegrationStageConfig) stageElementConfig.getStageType();
    } else {
      throw new CIStageExecutionException("Invalid stage type: " + stageElementConfig.getStageType());
    }
  }

  public ParallelStepElementConfig getParallelStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getParallel().toString(), ParallelStepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig parallel node", ex);
    }
  }

  public StepElementConfig getStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getStep().toString(), StepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig step node", ex);
    }
  }
  public CodeBase getCiCodeBase(YamlNode ciCodeBase) {
    try {
      return YamlUtils.read(ciCodeBase.toString(), CodeBase.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
  }

  public ExecutionSource buildExecutionSource(
      ExecutionMetadata executionMetadata, String identifier, ParameterField<Build> parameterFieldBuild) {
    ExecutionTriggerInfo executionTriggerInfo = executionMetadata.getTriggerInfo();

    if (executionTriggerInfo.getTriggerType() == TriggerType.MANUAL) {
      if (parameterFieldBuild == null) {
        return ManualExecutionSource.builder().build();
      }
      Build build = RunTimeInputHandler.resolveBuild(parameterFieldBuild);
      if (build != null) {
        if (build.getType().equals(BuildType.TAG)) {
          ParameterField<String> tag = ((TagBuildSpec) build.getSpec()).getTag();
          String buildString = RunTimeInputHandler.resolveStringParameter("tag", "Git Clone", identifier, tag, false);
          return ManualExecutionSource.builder().tag(buildString).build();
        } else if (build.getType().equals(BuildType.BRANCH)) {
          ParameterField<String> branch = ((BranchBuildSpec) build.getSpec()).getBranch();
          String branchString =
              RunTimeInputHandler.resolveStringParameter("branch", "Git Clone", identifier, branch, false);
          return ManualExecutionSource.builder().branch(branchString).build();
        }
      }
    } else if (executionTriggerInfo.getTriggerType() == TriggerType.WEBHOOK) {
      ParsedPayload parsedPayload = executionMetadata.getTriggerPayload().getParsedPayload();
      if (parsedPayload != null) {
        return WebhookTriggerProcessorUtils.convertWebhookResponse(parsedPayload);
      } else {
        throw new CIStageExecutionException("Parsed payload is empty for webhook execution");
      }
    } else if (executionTriggerInfo.getTriggerType() == TriggerType.WEBHOOK_CUSTOM) {
      return buildCustomExecutionSource(identifier, parameterFieldBuild);
    }

    return null;
  }

  public ImageDetails getImageInfo(String image) {
    String tag = "";
    String name = image;

    if (image.contains(IMAGE_PATH_SPLIT_REGEX)) {
      String[] subTokens = image.split(IMAGE_PATH_SPLIT_REGEX);
      if (subTokens.length > 1) {
        tag = subTokens[subTokens.length - 1];
        String[] nameparts = Arrays.copyOf(subTokens, subTokens.length - 1);
        name = String.join(IMAGE_PATH_SPLIT_REGEX, nameparts);
      }
    }

    return ImageDetails.builder().name(name).tag(tag).build();
  }

  private CustomExecutionSource buildCustomExecutionSource(
      String identifier, ParameterField<Build> parameterFieldBuild) {
    if (parameterFieldBuild == null) {
      return CustomExecutionSource.builder().build();
    }
    Build build = RunTimeInputHandler.resolveBuild(parameterFieldBuild);
    if (build != null) {
      if (build.getType().equals(BuildType.TAG)) {
        ParameterField<String> tag = ((TagBuildSpec) build.getSpec()).getTag();
        String buildString = RunTimeInputHandler.resolveStringParameter("tag", "Git Clone", identifier, tag, false);
        return CustomExecutionSource.builder().tag(buildString).build();
      } else if (build.getType().equals(BuildType.BRANCH)) {
        ParameterField<String> branch = ((BranchBuildSpec) build.getSpec()).getBranch();
        String branchString =
            RunTimeInputHandler.resolveStringParameter("branch", "Git Clone", identifier, branch, false);
        return CustomExecutionSource.builder().branch(branchString).build();
      }
    }
    return null;
  }
}

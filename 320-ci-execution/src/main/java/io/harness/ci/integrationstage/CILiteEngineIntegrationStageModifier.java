package io.harness.ci.integrationstage;

import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.yaml.ParameterField;
import io.harness.util.WebhookTriggerProcessorUtils;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.BuildType;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps for setting up pod and
 * adding cleanup
 */

@Slf4j
@Singleton
public class CILiteEngineIntegrationStageModifier implements StageExecutionModifier {
  @Inject private CILiteEngineStepGroupUtils ciLiteEngineStepGroupUtils;

  @Override
  public ExecutionElementConfig modifyExecutionPlan(ExecutionElementConfig execution,
      StageElementConfig stageElementConfig, PlanCreationContext context, String podName, CodeBase ciCodeBase) {
    log.info("Modifying execution plan to add lite engine step for integration stage {}",
        stageElementConfig.getIdentifier());

    PlanCreationContextValue planCreationContextValue = context.getGlobalContext().get("metadata");
    ExecutionMetadata executionMetadata = planCreationContextValue.getMetadata();

    CIExecutionArgs ciExecutionArgs =
        CIExecutionArgs.builder()
            .executionSource(
                buildExecutionSource(executionMetadata, stageElementConfig.getIdentifier(), ciCodeBase.getBuild()))
            .buildNumberDetails(
                BuildNumberDetails.builder().buildNumber((long) executionMetadata.getRunSequence()).build())
            .build();

    log.info("Build execution args for integration stage  {}", stageElementConfig.getIdentifier());
    return getCILiteEngineTaskExecution(stageElementConfig, ciExecutionArgs, ciCodeBase, podName, execution.getUuid());
  }

  private ExecutionSource buildExecutionSource(
      ExecutionMetadata executionMetadata, String identifier, ParameterField<Build> parameterFieldBuild) {
    ExecutionTriggerInfo executionTriggerInfo = executionMetadata.getTriggerInfo();

    if (executionTriggerInfo.getTriggerType() == TriggerType.MANUAL) {
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
    }

    return null;
  }

  private ExecutionElementConfig getCILiteEngineTaskExecution(StageElementConfig integrationStage,
      CIExecutionArgs ciExecutionArgs, CodeBase ciCodebase, String podName, String uuid) {
    return ExecutionElementConfig.builder()
        .uuid(uuid)
        .steps(ciLiteEngineStepGroupUtils.createExecutionWrapperWithLiteEngineSteps(
            integrationStage, ciExecutionArgs, ciCodebase, podName))
        .build();
  }
}

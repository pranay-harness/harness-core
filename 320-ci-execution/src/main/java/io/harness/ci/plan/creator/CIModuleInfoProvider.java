package io.harness.ci.plan.creator;

import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.ci.plan.creator.execution.CIPipelineModuleInfo;
import io.harness.ci.plan.creator.execution.CIStageModuleInfo;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.states.LiteEngineTaskStep;
import io.harness.util.WebhookTriggerProcessorUtils;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.BuildType;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CIModuleInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Inject OutcomeService outcomeService;

  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    String branch = null;
    String tag = null;

    if (isLiteEngineNodeAndCompleted(nodeExecutionProto.getNode())) {
      try {
        LiteEngineTaskStepInfo liteEngineTaskStepInfo =
            YamlUtils.read(nodeExecutionProto.getResolvedStepParameters(), LiteEngineTaskStepInfo.class);

        ParameterField<Build> buildParameterField = liteEngineTaskStepInfo.getCiCodebase().getBuild();
        Build build = RunTimeInputHandler.resolveBuild(buildParameterField);
        if (build != null && build.getType().equals(BuildType.BRANCH)) {
          branch = (String) ((BranchBuildSpec) build.getSpec()).getBranch().fetchFinalValue();
        }

        if (build != null && build.getType().equals(BuildType.TAG)) {
          tag = (String) ((TagBuildSpec) build.getSpec()).getTag().fetchFinalValue();
        }
      } catch (Exception ex) {
        log.error("Failed to retrieve branch and tag for filtering", ex);
      }
    }

    ExecutionSource executionSource = null;
    try {
      executionSource = getWebhookExecutionSource(nodeExecutionProto.getAmbiance().getMetadata());
    } catch (Exception ex) {
      log.error("Failed to retrieve branch and tag for filtering", ex);
    }
    return CIPipelineModuleInfo.builder()
        .branch(branch)
        .tag(tag)
        .ciExecutionInfoDTO(CIModuleInfoMapper.getCIBuildResponseDTO(executionSource))
        .build();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    return CIStageModuleInfo.builder().build();
  }

  private ExecutionSource getWebhookExecutionSource(ExecutionMetadata executionMetadata) {
    ExecutionTriggerInfo executionTriggerInfo = executionMetadata.getTriggerInfo();
    if (executionTriggerInfo.getTriggerType() == TriggerType.WEBHOOK) {
      ParsedPayload parsedPayload = executionMetadata.getTriggerPayload().getParsedPayload();
      if (parsedPayload != null) {
        return WebhookTriggerProcessorUtils.convertWebhookResponse(parsedPayload);
      } else {
        throw new CIStageExecutionException("Parsed payload is empty for webhook execution");
      }
    }
    return null;
  }

  private boolean isLiteEngineNodeAndCompleted(PlanNodeProto node) {
    return Objects.equals(node.getStepType().getType(), LiteEngineTaskStep.STEP_TYPE.getType());
  }
}

package io.harness.ci.plan.creator;

import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.ci.plan.creator.execution.CIPipelineModuleInfo;
import io.harness.ci.plan.creator.execution.CIStageModuleInfo;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.states.LiteEngineTaskStep;
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
      } catch (Exception exception) {
        log.error("Failed to retrieve branch and tag for filtering");
      }
    }
    // TODO retrieve executionSource here
    ExecutionSource executionSource = null; // = nodeExecutionProto.getAmbiance().;
    return CIPipelineModuleInfo.builder()
        .branch(branch)
        .tag(tag)
        .ciExecutionInfoDTO(CIModuleInfoMapper.getCIBuildResponseDTO(executionSource))
        .build();
  }

  private boolean isLiteEngineNodeAndCompleted(PlanNodeProto node) {
    return Objects.equals(node.getStepType().getType(), LiteEngineTaskStep.STEP_TYPE.getType());
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(NodeExecutionProto nodeExecutionProto) {
    // This one is for displaying tool tip
    return CIStageModuleInfo.builder().build();
  }
}

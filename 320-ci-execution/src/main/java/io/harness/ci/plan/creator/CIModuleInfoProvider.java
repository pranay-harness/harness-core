package io.harness.ci.plan.creator;

import static io.harness.git.GitClientHelper.getGitRepo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.ci.plan.creator.execution.CIPipelineModuleInfo;
import io.harness.ci.plan.creator.execution.CIStageModuleInfo;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;
import io.harness.states.LiteEngineTaskStep;
import io.harness.stateutils.buildstate.ConnectorUtils;
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
@OwnedBy(HarnessTeam.CI)
public class CIModuleInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Inject OutcomeService outcomeService;
  @Inject private ConnectorUtils connectorUtils;

  @Override
  public boolean shouldRun(OrchestrationEvent event) {
    return isLiteEngineNode(AmbianceUtils.getCurrentStepType(event.getAmbiance()));
  }

  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(OrchestrationEvent event) {
    String branch = null;
    String tag = null;
    String repoName = null;
    if (!isLiteEngineNode(AmbianceUtils.getCurrentStepType(event.getAmbiance()))) {
      return null;
    }

    Ambiance ambiance = event.getAmbiance();
    BaseNGAccess baseNGAccess = retrieveBaseNGAccess(ambiance);
    try {
      StepElementParameters stepElementParameters = (StepElementParameters) event.getResolvedStepParameters();
      LiteEngineTaskStepInfo liteEngineTaskStepInfo = (LiteEngineTaskStepInfo) stepElementParameters.getSpec();

      if (liteEngineTaskStepInfo == null) {
        return null;
      }

      ParameterField<Build> buildParameterField = null;
      if (liteEngineTaskStepInfo.getCiCodebase() != null) {
        buildParameterField = liteEngineTaskStepInfo.getCiCodebase().getBuild();

        if (liteEngineTaskStepInfo.getCiCodebase().getRepoName() != null) {
          repoName = liteEngineTaskStepInfo.getCiCodebase().getRepoName();
        } else if (liteEngineTaskStepInfo.getCiCodebase().getConnectorRef() != null) {
          try {
            ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(
                baseNGAccess, liteEngineTaskStepInfo.getCiCodebase().getConnectorRef());
            repoName = getGitRepo(connectorUtils.retrieveURL(connectorDetails));
          } catch (Exception exception) {
            log.warn("Failed to retrieve repo");
          }
        }
      }

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
    ExecutionSource executionSource = null;
    try {
      executionSource = getWebhookExecutionSource(event.getAmbiance().getMetadata(), event.getTriggerPayload());
    } catch (Exception ex) {
      log.error("Failed to retrieve branch and tag for filtering", ex);
    }
    return CIPipelineModuleInfo.builder()
        .branch(branch)
        .tag(tag)
        .repoName(repoName)
        .ciExecutionInfoDTO(CIModuleInfoMapper.getCIBuildResponseDTO(executionSource))
        .build();
  }

  @Override
  public StageModuleInfo getStageLevelModuleInfo(OrchestrationEvent event) {
    return CIStageModuleInfo.builder().build();
  }

  private ExecutionSource getWebhookExecutionSource(
      ExecutionMetadata executionMetadata, TriggerPayload triggerPayload) {
    ExecutionTriggerInfo executionTriggerInfo = executionMetadata.getTriggerInfo();
    if (executionTriggerInfo.getTriggerType() == TriggerType.WEBHOOK) {
      ParsedPayload parsedPayload = triggerPayload.getParsedPayload();
      if (parsedPayload != null) {
        return WebhookTriggerProcessorUtils.convertWebhookResponse(parsedPayload);
      } else {
        throw new CIStageExecutionException("Parsed payload is empty for webhook execution");
      }
    }
    return null;
  }

  // StepType
  private boolean isLiteEngineNode(StepType stepType) {
    return Objects.equals(stepType.getType(), LiteEngineTaskStep.STEP_TYPE.getType());
  }

  private BaseNGAccess retrieveBaseNGAccess(Ambiance ambiance) {
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    String accountId = AmbianceUtils.getAccountId(ambiance);

    return BaseNGAccess.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}

package io.harness.ci.plan.creator;

import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODEBASE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.git.GitClientHelper.getGitRepo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.CodebaseSweepingOutput;
import io.harness.ci.pipeline.executions.beans.CIBuildAuthor;
import io.harness.ci.pipeline.executions.beans.CIBuildCommit;
import io.harness.ci.pipeline.executions.beans.CIBuildPRHook;
import io.harness.ci.pipeline.executions.beans.CIWebhookInfoDTO;
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
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;
import io.harness.pms.sdk.execution.beans.StageModuleInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.states.LiteEngineTaskStep;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.util.WebhookTriggerProcessorUtils;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.BuildType;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIModuleInfoProvider implements ExecutionSummaryModuleInfoProvider {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private ConnectorUtils connectorUtils;

  @Override
  public boolean shouldRun(OrchestrationEvent event) {
    StepType currentStepType = AmbianceUtils.getCurrentStepType(event.getAmbiance());
    return currentStepType != null && isLiteEngineNode(currentStepType);
  }

  @Override
  public PipelineModuleInfo getPipelineLevelModuleInfo(OrchestrationEvent event) {
    String branch = null;
    String tag = null;
    String repoName = null;

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

    // get codebase sweeping output
    OptionalSweepingOutput optionalSweepingOutput =
        executionSweepingOutputService.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(CODEBASE));
    CodebaseSweepingOutput codebaseSweepingOutput = null;
    if (optionalSweepingOutput.isFound()) {
      codebaseSweepingOutput = (CodebaseSweepingOutput) optionalSweepingOutput.getOutput();
    }
    if (codebaseSweepingOutput != null) {
      log.info("Codebase sweeping output {}", codebaseSweepingOutput);

      List<CIBuildCommit> ciBuildCommits = new ArrayList<>();
      if (isNotEmpty(codebaseSweepingOutput.getCommits())) {
        for (CodebaseSweepingOutput.CodeBaseCommit commit : codebaseSweepingOutput.getCommits()) {
          ciBuildCommits.add(CIBuildCommit.builder()
                                 .id(commit.getId())
                                 .link(commit.getLink())
                                 .message(commit.getMessage())
                                 .ownerEmail(commit.getOwnerEmail())
                                 .ownerId(commit.getOwnerId())
                                 .ownerName(commit.getOwnerName())
                                 .timeStamp(commit.getTimeStamp())
                                 .build());
        }
      }
      if (isEmpty(branch)) {
        branch = codebaseSweepingOutput.getBranch();
      }

      return CIPipelineModuleInfo.builder()
          .branch(branch)
          .repoName(repoName)
          .ciExecutionInfoDTO(getCiExecutionInfoDTO(codebaseSweepingOutput, ciBuildCommits))
          .build();
    }

    return CIPipelineModuleInfo.builder()
        .branch(branch)
        .tag(tag)
        .repoName(repoName)
        .ciExecutionInfoDTO(CIModuleInfoMapper.getCIBuildResponseDTO(executionSource))
        .build();
  }

  private CIWebhookInfoDTO getCiExecutionInfoDTO(
      CodebaseSweepingOutput codebaseSweepingOutput, List<CIBuildCommit> ciBuildCommits) {
    if (isEmpty(codebaseSweepingOutput.getCommits())) {
      return null;
    }
    return CIWebhookInfoDTO.builder()
        .event("pullRequest")
        .author(CIBuildAuthor.builder()
                    .name(codebaseSweepingOutput.getGitUserName())
                    .avatar(codebaseSweepingOutput.getGitUserAvatar())
                    .email(codebaseSweepingOutput.getGitUserEmail())
                    .id(codebaseSweepingOutput.getGitUserId())
                    .build())
        .pullRequest(CIBuildPRHook.builder()
                         .id(Long.valueOf(codebaseSweepingOutput.getPrNumber()))
                         .link(codebaseSweepingOutput.getPullRequestLink())
                         .title(codebaseSweepingOutput.getPrTitle())
                         .body(codebaseSweepingOutput.getPullRequestBody())
                         .sourceBranch(codebaseSweepingOutput.getSourceBranch())
                         .targetBranch(codebaseSweepingOutput.getTargetBranch())
                         .state(codebaseSweepingOutput.getState())
                         .commits(ciBuildCommits)
                         .build())

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
      if (triggerPayload != null) {
        ParsedPayload parsedPayload = triggerPayload.getParsedPayload();
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

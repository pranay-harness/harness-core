package io.harness.pms.triggers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.ngtriggers.Constants.PR;
import static io.harness.ngtriggers.Constants.PUSH;
import static io.harness.ngtriggers.Constants.TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK_CUSTOM;
import static io.harness.pms.contracts.triggers.Type.CUSTOM;
import static io.harness.pms.plan.execution.PlanExecutionInterruptType.ABORT;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.TriggerException;
import io.harness.execution.PlanExecution;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.target.TargetSpec;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.contracts.triggers.Type;
import io.harness.pms.helpers.PrincipalInfoHelper;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.PipelineExecuteHelper;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.PushHook;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerExecutionHelper {
  private final PMSPipelineService pmsPipelineService;
  private final PipelineExecuteHelper pipelineExecuteHelper;
  private final PlanExecutionService planExecutionService;
  private final PMSExecutionService pmsExecutionService;

  public PlanExecution resolveRuntimeInputAndSubmitExecutionRequest(
      TriggerDetails triggerDetails, TriggerPayload triggerPayload) {
    String executionTagForGitEvent = generateExecutionTagForEvent(triggerDetails, triggerPayload);
    TriggeredBy embeddedUser = generateTriggerdBy(executionTagForGitEvent);

    TriggerType triggerType = findTriggerType(triggerPayload);
    ExecutionTriggerInfo triggerInfo =
        ExecutionTriggerInfo.newBuilder().setTriggerType(triggerType).setTriggeredBy(embeddedUser).build();
    try {
      NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
      String targetIdentifier = ngTriggerEntity.getTargetIdentifier();
      Optional<PipelineEntity> pipelineEntityToExecute =
          pmsPipelineService.incrementRunSequence(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
              ngTriggerEntity.getProjectIdentifier(), targetIdentifier, false);

      if (!pipelineEntityToExecute.isPresent()) {
        throw new TriggerException("Unable to continue trigger execution. Pipeline with identifier: "
                + ngTriggerEntity.getTargetIdentifier() + ", with org: " + ngTriggerEntity.getOrgIdentifier()
                + ", with ProjectId: " + ngTriggerEntity.getProjectIdentifier()
                + ", For Trigger: " + ngTriggerEntity.getIdentifier() + " does not exists. ",
            USER);
      }

      String runtimeInputYaml = readRuntimeInputFromConfig(triggerDetails.getNgTriggerConfig());

      ExecutionMetadata.Builder executionMetaDataBuilder =
          ExecutionMetadata.newBuilder()
              .setExecutionUuid(generateUuid())
              .setTriggerInfo(triggerInfo)
              .setRunSequence(pipelineEntityToExecute.get().getRunSequence())
              .setTriggerPayload(triggerPayload)
              .setPipelineIdentifier(pipelineEntityToExecute.get().getIdentifier());

      String pipelineYaml;
      if (isBlank(runtimeInputYaml)) {
        pipelineYaml = pipelineEntityToExecute.get().getYaml();
      } else {
        String pipelineYamlBeforeMerge = pipelineEntityToExecute.get().getYaml();
        String sanitizedRuntimeInputYaml = MergeHelper.sanitizeRuntimeInput(pipelineYamlBeforeMerge, runtimeInputYaml);
        if (isBlank(sanitizedRuntimeInputYaml)) {
          pipelineYaml = pipelineYamlBeforeMerge;
        } else {
          executionMetaDataBuilder.setInputSetYaml(sanitizedRuntimeInputYaml);
          pipelineYaml =
              MergeHelper.mergeInputSetIntoPipeline(pipelineYamlBeforeMerge, sanitizedRuntimeInputYaml, true);
        }
      }
      executionMetaDataBuilder.setYaml(pipelineYaml);
      executionMetaDataBuilder.setPrincipalInfo(PrincipalInfoHelper.getPrincipalInfoFromSecurityContext());

      return pipelineExecuteHelper.startExecution(ngTriggerEntity.getAccountId(), ngTriggerEntity.getOrgIdentifier(),
          ngTriggerEntity.getProjectIdentifier(), pipelineYaml, executionMetaDataBuilder.build());
    } catch (Exception e) {
      throw new TriggerException("Failed while requesting Pipeline Execution" + e.getMessage(), USER);
    }
  }

  @VisibleForTesting
  TriggeredBy generateTriggerdBy(String executionTagForGitEvent) {
    TriggeredBy.Builder builder = TriggeredBy.newBuilder().setIdentifier("trigger").setUuid("systemUser");
    if (isNotBlank(executionTagForGitEvent)) {
      builder.putExtraInfo(PlanExecution.EXEC_TAG_SET_BY_TRIGGER, executionTagForGitEvent);
    }
    return builder.build();
  }

  private String readRuntimeInputFromConfig(NGTriggerConfig ngTriggerConfig) {
    TargetSpec targetSpec = ngTriggerConfig.getTarget().getSpec();
    PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
    return pipelineTargetSpec.getRuntimeInputYaml();
  }

  private TriggerType findTriggerType(TriggerPayload triggerPayload) {
    TriggerType triggerType = WEBHOOK;
    if (triggerPayload.getType() == CUSTOM) {
      triggerType = WEBHOOK_CUSTOM;
    } else if (triggerPayload.getType() == Type.SCHEDULED) {
      triggerType = TriggerType.SCHEDULER_CRON;
    }

    return triggerType;
  }

  /**
   * Generate execution tag to identify pipeline executions caused by similar trigger git events.
   * PR: {accId:orgId:projectId:pipelineIdentifier:triggerId:PR:RepoUrl:PrNum:SourceBranch:TargetBranch}
   * PUSH: {accId:orgId:projectId:pipelineIdentifier:triggerId:PUSH:RepoUrl:Ref}
   *
   * @param triggerDetails
   * @param triggerPayload
   * @return
   */
  public String generateExecutionTagForEvent(TriggerDetails triggerDetails, TriggerPayload triggerPayload) {
    String triggerRef = new StringBuilder(256)
                            .append(triggerDetails.getNgTriggerEntity().getAccountId())
                            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
                            .append(triggerDetails.getNgTriggerEntity().getOrgIdentifier())
                            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
                            .append(triggerDetails.getNgTriggerEntity().getProjectIdentifier())
                            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
                            .append(triggerDetails.getNgTriggerEntity().getTargetIdentifier())
                            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
                            .append(triggerDetails.getNgTriggerEntity().getIdentifier())
                            .toString();

    try {
      if (!triggerPayload.hasParsedPayload()) {
        return triggerRef;
      }

      ParsedPayload parsedPayload = triggerPayload.getParsedPayload();
      StringBuilder executionTag = new StringBuilder(512).append(triggerRef);

      if (parsedPayload.hasPr()) {
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER).append(PR);
        PullRequestHook pullRequestHook = parsedPayload.getPr();
        PullRequest pr = pullRequestHook.getPr();
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pullRequestHook.getRepo().getLink())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pr.getNumber())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pr.getSource())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pr.getTarget());
      } else if (parsedPayload.hasPush()) {
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER).append(PUSH);
        PushHook pushHook = parsedPayload.getPush();
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pushHook.getRepo().getLink())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pushHook.getRef());
      }
      return executionTag.toString();
    } catch (Exception e) {
      log.error("failed to generate complete Execution Tag for Trigger: " + triggerRef, e);
    }

    return triggerRef;
  }

  @VisibleForTesting
  void requestPipelineExecutionAbortForSameExecTagIfNeeded(PlanExecution planExecution, String executionTag) {
    try {
      List<PlanExecution> executionsToAbort =
          planExecutionService.findPrevUnTerminatedPlanExecutionsByExecutionTag(planExecution, executionTag);
      if (isEmpty(executionsToAbort)) {
        return;
      }

      for (PlanExecution execution : executionsToAbort) {
        registerPipelineExecutionAbortInterrupt(execution, executionTag);
      }
    } catch (Exception e) {
      log.error("Failed while requesting abort for pipeline executions using executionTag: " + executionTag);
    }
  }

  private void registerPipelineExecutionAbortInterrupt(PlanExecution execution, String executionTag) {
    try {
      log.info(new StringBuilder(128)
                   .append("Requesting Pipeline Execution Abort for planExecutionId")
                   .append(execution.getUuid())
                   .append(", with Tag: ")
                   .append(executionTag)
                   .toString());
      pmsExecutionService.registerInterrupt(ABORT, execution.getUuid(), null);
    } catch (Exception e) {
      log.error("Exception white requesting Pipeline Execution Abort: " + executionTag, e);
    }
  }
}

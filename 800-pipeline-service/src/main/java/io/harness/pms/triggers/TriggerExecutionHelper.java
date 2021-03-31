package io.harness.pms.triggers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK_CUSTOM;
import static io.harness.pms.contracts.triggers.Type.CUSTOM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
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
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.helpers.PrincipalInfoHelper;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.PipelineExecuteHelper;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerExecutionHelper {
  private final PMSPipelineService pmsPipelineService;
  private final PipelineExecuteHelper pipelineExecuteHelper;

  public PlanExecution resolveRuntimeInputAndSubmitExecutionRequest(
      TriggerDetails triggerDetails, TriggerPayload triggerPayload) {
    TriggeredBy embeddedUser = TriggeredBy.newBuilder().setIdentifier("trigger").setUuid("systemUser").build();

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
      if (EmptyPredicate.isEmpty(runtimeInputYaml)) {
        pipelineYaml = pipelineEntityToExecute.get().getYaml();
      } else {
        String pipelineYamlBeforeMerge = pipelineEntityToExecute.get().getYaml();
        String sanitizedRuntimeInputYaml = MergeHelper.sanitizeRuntimeInput(pipelineYamlBeforeMerge, runtimeInputYaml);
        if (EmptyPredicate.isEmpty(sanitizedRuntimeInputYaml)) {
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

  private String readRuntimeInputFromConfig(NGTriggerConfig ngTriggerConfig) {
    TargetSpec targetSpec = ngTriggerConfig.getTarget().getSpec();
    PipelineTargetSpec pipelineTargetSpec = (PipelineTargetSpec) targetSpec;
    return pipelineTargetSpec.getRuntimeInputYaml();
  }

  private TriggerType findTriggerType(TriggerPayload triggerPayload) {
    TriggerType triggerType = WEBHOOK;
    if (triggerPayload.getType() == CUSTOM) {
      triggerType = WEBHOOK_CUSTOM;
    } // cron will come here

    return triggerType;
  }
}

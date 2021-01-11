package io.harness.pms.plan.execution;

import io.harness.data.algorithm.HashGenerator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.creation.PlanCreatorMergeService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class PipelineExecuteHelper {
  private final PMSPipelineService pmsPipelineService;
  private final OrchestrationService orchestrationService;
  private final PlanCreatorMergeService planCreatorMergeService;
  private final ValidateAndMergeHelper validateAndMergeHelper;

  public PlanExecution runPipelineWithInputSetPipelineYaml(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml,
      String eventPayload, ExecutionTriggerInfo triggerInfo) throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }

    String pipelineYaml;
    ExecutionMetadata.Builder executionMetadataBuilder = ExecutionMetadata.newBuilder()
                                                             .setTriggerInfo(triggerInfo)
                                                             .setRunSequence(pipelineEntity.get().getRunSequence());

    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml, true);
      executionMetadataBuilder.setInputSetYaml(inputSetPipelineYaml);
    }
    executionMetadataBuilder.setPipelineIdentifier(pipelineIdentifier);

    return startExecution(accountId, orgIdentifier, projectIdentifier, pipelineYaml, executionMetadataBuilder.build());
  }

  public PlanExecution runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetReferences,
      ExecutionTriggerInfo triggerInfo) throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }

    ExecutionMetadata.Builder executionMetadataBuilder = ExecutionMetadata.newBuilder()
                                                             .setTriggerInfo(triggerInfo)
                                                             .setRunSequence(pipelineEntity.get().getRunSequence());
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences);
    String pipelineYaml =
        MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), mergedRuntimeInputYaml, true);
    executionMetadataBuilder.setPipelineIdentifier(pipelineIdentifier);
    executionMetadataBuilder.setInputSetYaml(mergedRuntimeInputYaml);

    return startExecution(accountId, orgIdentifier, projectIdentifier, pipelineYaml, executionMetadataBuilder.build());
  }

  public PlanExecution startExecution(String accountId, String orgIdentifier, String projectIdentifier, String yaml,
      ExecutionMetadata executionMetadata) throws IOException {
    PlanCreationBlobResponse resp = planCreatorMergeService.createPlan(yaml, executionMetadata);
    Plan plan = PlanExecutionUtils.extractPlan(resp);
    ImmutableMap.Builder<String, String> abstractionsBuilder =
        ImmutableMap.<String, String>builder()
            .put(SetupAbstractionKeys.accountId, accountId)
            .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
            .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier)
            .put("expressionFunctorToken", Integer.toString(HashGenerator.generateIntegerHash()));

    if (executionMetadata.hasTriggerPayload()) {
      abstractionsBuilder.put(
          SetupAbstractionKeys.eventPayload, executionMetadata.getTriggerPayload().getJsonPayload());
    }

    return orchestrationService.startExecution(plan, abstractionsBuilder.build(), executionMetadata);
  }
}

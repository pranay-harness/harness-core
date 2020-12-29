package io.harness.pms.plan.execution;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    Map<String, Object> contextAttributes = new HashMap<>();
    contextAttributes.put(SetupAbstractionKeys.eventPayload, eventPayload);
    String pipelineYaml;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml);
      contextAttributes.put(SetupAbstractionKeys.inputSetYaml, inputSetPipelineYaml);
    }
    contextAttributes.put(SetupAbstractionKeys.pipelineIdentifier, pipelineIdentifier);
    contextAttributes.put(SetupAbstractionKeys.runSequence, pipelineEntity.get().getRunSequence());
    return startExecution(accountId, orgIdentifier, projectIdentifier, pipelineYaml, triggerInfo, contextAttributes);
  }

  public PlanExecution runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetReferences,
      ExecutionTriggerInfo triggerInfo) throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }

    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences);
    String pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), mergedRuntimeInputYaml);
    Map<String, Object> contextAttributes = new HashMap<>();
    contextAttributes.put(SetupAbstractionKeys.inputSetYaml, mergedRuntimeInputYaml);
    contextAttributes.put(SetupAbstractionKeys.pipelineIdentifier, pipelineIdentifier);
    contextAttributes.put(SetupAbstractionKeys.runSequence, pipelineEntity.get().getRunSequence());

    return startExecution(accountId, orgIdentifier, projectIdentifier, pipelineYaml, triggerInfo, contextAttributes);
  }

  public PlanExecution startExecution(String accountId, String orgIdentifier, String projectIdentifier, String yaml,
      ExecutionTriggerInfo triggerInfo, Map<String, Object> contextAttributes) throws IOException {
    ExecutionMetadata metadata =
        ExecutionMetadata.newBuilder()
            .setRunSequence((Integer) contextAttributes.getOrDefault(SetupAbstractionKeys.runSequence, 0))
            .setTriggerInfo(triggerInfo)
            .build();
    PlanCreationBlobResponse resp = planCreatorMergeService.createPlan(yaml, contextAttributes, metadata);
    Plan plan = PlanExecutionUtils.extractPlan(resp);
    ImmutableMap.Builder<String, String> abstractionsBuilder =
        ImmutableMap.<String, String>builder()
            .put(SetupAbstractionKeys.accountId, accountId)
            .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
            .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier);

    if (isNotEmpty(contextAttributes)) {
      contextAttributes.forEach((key, val) -> {
        if (val != null && String.class.isAssignableFrom(val.getClass())) {
          abstractionsBuilder.put(key, (String) val);
        }
      });
    }
    return orchestrationService.startExecution(plan, abstractionsBuilder.build(), metadata);
  }
}

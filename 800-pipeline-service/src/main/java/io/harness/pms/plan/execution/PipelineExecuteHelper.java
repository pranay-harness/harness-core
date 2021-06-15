package io.harness.pms.plan.execution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.helpers.PrincipalInfoHelper;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.plan.creation.PlanCreatorMergeService;
import io.harness.pms.rbac.validator.PipelineRbacService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class PipelineExecuteHelper {
  private final PMSPipelineService pmsPipelineService;
  private final OrchestrationService orchestrationService;
  private final PlanCreatorMergeService planCreatorMergeService;
  private final ValidateAndMergeHelper validateAndMergeHelper;
  private final PipelineRbacService pipelineRbacServiceImpl;
  private final PrincipalInfoHelper principalInfoHelper;
  private final PMSYamlSchemaService pmsYamlSchemaService;
  private final PmsGitSyncHelper pmsGitSyncHelper;

  public PlanExecutionResponseDto runPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, String inputSetPipelineYaml, ExecutionTriggerInfo triggerInfo) throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }

    final String executionId = generateUuid();
    String pipelineYaml;
    ExecutionMetadata.Builder executionMetadataBuilder = ExecutionMetadata.newBuilder()
                                                             .setExecutionUuid(executionId)
                                                             .setTriggerInfo(triggerInfo)
                                                             .setModuleType(moduleType)
                                                             .setRunSequence(pipelineEntity.get().getRunSequence());

    PlanExecutionMetadata.Builder planExecutionMetadataBuilder =
        PlanExecutionMetadata.builder().planExecutionId(executionId);

    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml, true);
      planExecutionMetadataBuilder.inputSetYaml(inputSetPipelineYaml);
    }
    planExecutionMetadataBuilder.yaml(pipelineYaml);

    pmsYamlSchemaService.validateYamlSchema(accountId, orgIdentifier, projectIdentifier, pipelineYaml);

    pipelineRbacServiceImpl.extractAndValidateStaticallyReferredEntities(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);
    executionMetadataBuilder.setPipelineIdentifier(pipelineIdentifier);
    executionMetadataBuilder.setPrincipalInfo(principalInfoHelper.getPrincipalInfoFromSecurityContext());

    PlanExecution planExecution = startExecution(accountId, orgIdentifier, projectIdentifier, pipelineYaml,
        executionMetadataBuilder.build(), planExecutionMetadataBuilder, null);
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity.get()))
        .build();
  }

  public PlanExecutionResponseDto runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, List<String> inputSetReferences,
      String pipelineBranch, String pipelineRepoID, ExecutionTriggerInfo triggerInfo) throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }

    final String executionId = generateUuid();
    ExecutionMetadata.Builder executionMetadataBuilder = ExecutionMetadata.newBuilder()
                                                             .setExecutionUuid(executionId)
                                                             .setTriggerInfo(triggerInfo)
                                                             .setModuleType(moduleType)
                                                             .setRunSequence(pipelineEntity.get().getRunSequence());

    PlanExecutionMetadata.Builder planExecutionMetadataBuilder =
        PlanExecutionMetadata.builder().planExecutionId(executionId);

    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId,
        orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID);
    String pipelineYaml =
        MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), mergedRuntimeInputYaml, true);

    planExecutionMetadataBuilder.yaml(pipelineYaml).inputSetYaml(mergedRuntimeInputYaml);

    pmsYamlSchemaService.validateYamlSchema(accountId, orgIdentifier, projectIdentifier, pipelineYaml);

    pipelineRbacServiceImpl.extractAndValidateStaticallyReferredEntities(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);

    executionMetadataBuilder.setPipelineIdentifier(pipelineIdentifier);
    executionMetadataBuilder.setPrincipalInfo(principalInfoHelper.getPrincipalInfoFromSecurityContext());

    PlanExecution planExecution = startExecution(accountId, orgIdentifier, projectIdentifier, pipelineYaml,
        executionMetadataBuilder.build(), planExecutionMetadataBuilder, null);
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity.get()))
        .build();
  }

  public PlanExecution startExecution(String accountId, String orgIdentifier, String projectIdentifier, String yaml,
      ExecutionMetadata executionMetadata, PlanExecutionMetadata.Builder planExecutionMetadataBuilder,
      TriggerPayload triggerPayload) throws IOException {
    ExecutionMetadata.Builder executionMetadataBuilder = ExecutionMetadata.newBuilder(executionMetadata);
    // Set git sync branch context in execute metadata. This will be used for plan creation and execution.
    ByteString gitSyncBranchContext = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    if (gitSyncBranchContext != null) {
      executionMetadataBuilder.setGitSyncBranchContext(gitSyncBranchContext);
    }

    ExecutionMetadata enhancedExecutionMetadata = executionMetadataBuilder.build();

    PlanCreationBlobResponse resp = planCreatorMergeService.createPlan(
        yaml, enhancedExecutionMetadata, planExecutionMetadataBuilder, triggerPayload);
    planExecutionMetadataBuilder.triggerPayload(triggerPayload);
    Plan plan = PlanExecutionUtils.extractPlan(resp);
    ImmutableMap.Builder<String, String> abstractionsBuilder =
        ImmutableMap.<String, String>builder()
            .put(SetupAbstractionKeys.accountId, accountId)
            .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
            .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier);

    return orchestrationService.startExecution(
        plan, abstractionsBuilder.build(), enhancedExecutionMetadata, planExecutionMetadataBuilder.build());
  }
}

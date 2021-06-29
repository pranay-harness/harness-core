package io.harness.pms.plan.execution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.plan.TriggerType.MANUAL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationService;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.execution.PlanExecutionMetadata.Builder;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.RerunInfo;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.helpers.PrincipalInfoHelper;
import io.harness.pms.helpers.TriggeredByHelper;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.plan.creation.PlanCreatorMergeService;
import io.harness.pms.rbac.validator.PipelineRbacService;
import io.harness.pms.yaml.YamlUtils;

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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO (P0 | Prashant | Naman): Even after a lot of refactoring for this file its still messed up.
 * We need to more here. Also same logic is somewhat duplicated in TriggerExecution helper. That file needs to go
 *
 * THIS IS VERY BAD :(. We need to do better here
 *
 */

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
  private final PlanExecutionMetadataService planExecutionMetadataService;
  private final TriggeredByHelper triggeredByHelper;
  private final PlanExecutionService planExecutionService;

  public PlanExecutionResponseDto runPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, String inputSetPipelineYaml) throws IOException {
    PipelineEntity pipelineEntity =
        fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    ExecutionTriggerInfo triggerInfo = buildTriggerInfo(null);
    ExecArgs execArgs = buildExecutionArgs(pipelineEntity, moduleType, inputSetPipelineYaml, triggerInfo);

    PlanExecution planExecution =
        startExecution(accountId, orgIdentifier, projectIdentifier, execArgs.metadata, execArgs.planExecutionMetadata);
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }

  public PlanExecutionResponseDto rerunPipelineWithInputSetPipelineYaml(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, String originalExecutionId,
      String inputSetPipelineYaml) throws IOException {
    PipelineEntity pipelineEntity =
        fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);

    ExecutionTriggerInfo triggerInfo = buildTriggerInfo(originalExecutionId);

    ExecArgs execArgs = buildExecutionArgs(pipelineEntity, moduleType, inputSetPipelineYaml, triggerInfo);

    // TODO: this is Quick fix for CIGA :( we would need to refactor this later
    populateTriggerDataForRerun(originalExecutionId, execArgs);

    PlanExecution planExecution =
        startExecution(accountId, orgIdentifier, projectIdentifier, execArgs.metadata, execArgs.planExecutionMetadata);
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }

  private ExecutionTriggerInfo buildTriggerInfo(String originalExecutionId) {
    ExecutionTriggerInfo.Builder triggerInfoBuilder =
        ExecutionTriggerInfo.newBuilder().setTriggerType(MANUAL).setTriggeredBy(
            triggeredByHelper.getFromSecurityContext());

    if (originalExecutionId == null) {
      return triggerInfoBuilder.setIsRerun(false).build();
    }
    PlanExecution originalPlanExecution = planExecutionService.get(originalExecutionId);
    ExecutionTriggerInfo originalTriggerInfo = originalPlanExecution.getMetadata().getTriggerInfo();
    RerunInfo.Builder rerunInfoBuilder = RerunInfo.newBuilder()
                                             .setPrevExecutionId(originalExecutionId)
                                             .setPrevTriggerType(originalTriggerInfo.getTriggerType());
    if (originalTriggerInfo.getIsRerun()) {
      return triggerInfoBuilder.setIsRerun(true)
          .setRerunInfo(rerunInfoBuilder.setRootExecutionId(originalTriggerInfo.getRerunInfo().getRootExecutionId())
                            .setRootTriggerType(originalTriggerInfo.getRerunInfo().getRootTriggerType())
                            .build())
          .build();
    }

    return triggerInfoBuilder.setIsRerun(true)
        .setRerunInfo(rerunInfoBuilder.setRootExecutionId(originalExecutionId)
                          .setRootTriggerType(originalTriggerInfo.getTriggerType())
                          .build())
        .build();
  }

  public PlanExecutionResponseDto runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, List<String> inputSetReferences,
      String pipelineBranch, String pipelineRepoID) throws IOException {
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId,
        orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID);

    PipelineEntity pipelineEntity =
        fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);

    ExecutionTriggerInfo triggerInfo = buildTriggerInfo(null);
    ExecArgs execArgs = buildExecutionArgs(pipelineEntity, moduleType, mergedRuntimeInputYaml, triggerInfo);

    PlanExecution planExecution =
        startExecution(accountId, orgIdentifier, projectIdentifier, execArgs.metadata, execArgs.planExecutionMetadata);
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }

  public PlanExecutionResponseDto rerunPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, String originalExecutionId,
      List<String> inputSetReferences, String pipelineBranch, String pipelineRepoID) throws IOException {
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId,
        orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID);
    PipelineEntity pipelineEntity =
        fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);

    ExecutionTriggerInfo triggerInfo = buildTriggerInfo(originalExecutionId);

    ExecArgs execArgs = buildExecutionArgs(pipelineEntity, moduleType, mergedRuntimeInputYaml, triggerInfo);

    // TODO: this is Quick fix for CIGA :( we would need to refactor this later
    populateTriggerDataForRerun(originalExecutionId, execArgs);

    PlanExecution planExecution =
        startExecution(accountId, orgIdentifier, projectIdentifier, execArgs.metadata, execArgs.planExecutionMetadata);
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }

  void populateTriggerDataForRerun(String originalExecutionId, ExecArgs execArgs) {
    Optional<PlanExecutionMetadata> prevMetadataOptional =
        planExecutionMetadataService.findByPlanExecutionId(originalExecutionId);

    if (prevMetadataOptional.isPresent()) {
      PlanExecutionMetadata prevMetadata = prevMetadataOptional.get();
      execArgs.planExecutionMetadata =
          execArgs.planExecutionMetadata.withTriggerPayload(prevMetadata.getTriggerPayload())
              .withTriggerJsonPayload(prevMetadata.getTriggerJsonPayload());
    }
  }

  private String buildAndValidatePipelineYaml(String inputSetPipelineYaml, PipelineEntity pipelineEntity)
      throws IOException {
    String pipelineYaml;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.getYaml();
    } else {
      pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.getYaml(), inputSetPipelineYaml, true);
    }
    pmsYamlSchemaService.validateYamlSchema(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
        pipelineEntity.getProjectIdentifier(), pipelineYaml);
    pipelineRbacServiceImpl.extractAndValidateStaticallyReferredEntities(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(),
        pipelineYaml);
    return pipelineYaml;
  }

  private ExecArgs buildExecutionArgs(PipelineEntity pipelineEntity, String moduleType, String mergedRuntimeInputYaml,
      ExecutionTriggerInfo triggerInfo) throws IOException {
    final String executionId = generateUuid();

    // Build Execution Metadata
    ExecutionMetadata executionMetadata =
        buildExecutionMetadata(pipelineEntity.getIdentifier(), moduleType, triggerInfo, pipelineEntity, executionId);

    // Build PlanExecution Metadata
    String pipelineYaml = buildAndValidatePipelineYaml(mergedRuntimeInputYaml, pipelineEntity);

    PlanExecutionMetadata planExecutionMetadata =
        obtainMetadataBuilder(mergedRuntimeInputYaml, executionId, pipelineYaml);

    return ExecArgs.builder().metadata(executionMetadata).planExecutionMetadata(planExecutionMetadata).build();
  }

  private ExecutionMetadata buildExecutionMetadata(@NotNull String pipelineIdentifier, String moduleType,
      ExecutionTriggerInfo triggerInfo, PipelineEntity pipelineEntity, String executionId) {
    ExecutionMetadata.Builder builder =
        ExecutionMetadata.newBuilder()
            .setExecutionUuid(executionId)
            .setTriggerInfo(triggerInfo)
            .setModuleType(moduleType)
            .setRunSequence(pipelineEntity.getRunSequence())
            .setPipelineIdentifier(pipelineIdentifier)
            .setPrincipalInfo(principalInfoHelper.getPrincipalInfoFromSecurityContext());
    ByteString gitSyncBranchContext = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    if (gitSyncBranchContext != null) {
      builder.setGitSyncBranchContext(gitSyncBranchContext);
    }
    return builder.build();
  }

  private PipelineEntity fetchPipelineEntity(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier) {
    Optional<PipelineEntity> pipelineEntityOptional =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntityOptional.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }

    return pipelineEntityOptional.get();
  }

  private PlanExecutionMetadata obtainMetadataBuilder(
      String inputSetPipelineYaml, String executionId, String pipelineYaml) throws IOException {
    Builder planExecutionMetadataBuilder = PlanExecutionMetadata.builder().planExecutionId(executionId);
    planExecutionMetadataBuilder.inputSetYaml(inputSetPipelineYaml);
    planExecutionMetadataBuilder.yaml(pipelineYaml);
    planExecutionMetadataBuilder.processedYaml(YamlUtils.injectUuid(pipelineYaml));
    return planExecutionMetadataBuilder.build();
  }

  public PlanExecution startExecution(String accountId, String orgIdentifier, String projectIdentifier,
      ExecutionMetadata executionMetadata, PlanExecutionMetadata planExecutionMetadata) throws IOException {
    PlanCreationBlobResponse resp = planCreatorMergeService.createPlan(executionMetadata, planExecutionMetadata);
    Plan plan = PlanExecutionUtils.extractPlan(resp);
    ImmutableMap<String, String> abstractions = ImmutableMap.<String, String>builder()
                                                    .put(SetupAbstractionKeys.accountId, accountId)
                                                    .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
                                                    .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier)
                                                    .build();

    return orchestrationService.startExecution(plan, abstractions, executionMetadata, planExecutionMetadata);
  }

  @Data
  @lombok.Builder
  private static class ExecArgs {
    ExecutionMetadata metadata;
    PlanExecutionMetadata planExecutionMetadata;
  }
}

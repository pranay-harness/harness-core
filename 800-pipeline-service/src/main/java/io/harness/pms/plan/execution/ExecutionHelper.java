package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.plan.TriggerType.MANUAL;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationService;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.executions.retry.RetryExecutionHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.plan.IdentityPlanNode;
import io.harness.plan.Node;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.RerunInfo;
import io.harness.pms.exception.PmsExceptionUtils;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.helpers.PrincipalInfoHelper;
import io.harness.pms.helpers.TriggeredByHelper;
import io.harness.pms.merger.helpers.InputSetMergeHelper;
import io.harness.pms.ngpipeline.inputset.helpers.InputSetSanitizer;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.plan.creation.PlanCreatorMergeService;
import io.harness.pms.plan.execution.beans.ExecArgs;
import io.harness.pms.plan.execution.beans.StagesExecutionInfo;
import io.harness.pms.rbac.validator.PipelineRbacService;
import io.harness.pms.yaml.YamlUtils;
import io.harness.threading.Morpheus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class ExecutionHelper {
  PMSPipelineService pmsPipelineService;
  TriggeredByHelper triggeredByHelper;
  PlanExecutionService planExecutionService;
  PrincipalInfoHelper principalInfoHelper;
  PmsGitSyncHelper pmsGitSyncHelper;
  PMSYamlSchemaService pmsYamlSchemaService;
  PipelineRbacService pipelineRbacServiceImpl;
  RetryExecutionHelper retryExecutionHelper;
  PlanCreatorMergeService planCreatorMergeService;
  OrchestrationService orchestrationService;
  PlanService planService;
  PlanExecutionMetadataService planExecutionMetadataService;

  public PipelineEntity fetchPipelineEntity(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier) {
    Optional<PipelineEntity> pipelineEntityOptional =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntityOptional.isPresent()) {
      throw new InvalidRequestException(
          String.format("Pipeline with the given ID: %s does not exist or has been deleted", pipelineIdentifier));
    }
    return pipelineEntityOptional.get();
  }

  public ExecutionTriggerInfo buildTriggerInfo(String originalExecutionId) {
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

  public ExecArgs buildExecutionArgs(PipelineEntity pipelineEntity, String moduleType, String mergedRuntimeInputYaml,
      List<String> stagesToRun, ExecutionTriggerInfo triggerInfo, String originalExecutionId, boolean isRetry,
      String previousProcessedYaml, List<String> retryStagesIdentifier, List<String> uuidForSkipNode) {
    final String executionId = generateUuid();

    ExecutionMetadata executionMetadata =
        buildExecutionMetadata(pipelineEntity.getIdentifier(), moduleType, triggerInfo, pipelineEntity, executionId);

    String pipelineYaml = getPipelineYamlAndValidate(mergedRuntimeInputYaml, pipelineEntity);
    StagesExecutionInfo stagesExecutionInfo =
        StagesExecutionInfo.builder().isStagesExecution(false).pipelineYamlToRun(pipelineYaml).build();
    if (EmptyPredicate.isNotEmpty(stagesToRun)) {
      stagesExecutionInfo = StagesExecutionHelper.getStagesExecutionInfo(pipelineYaml, stagesToRun);
    }
    PlanExecutionMetadata planExecutionMetadata =
        obtainPlanExecutionMetadata(mergedRuntimeInputYaml, executionId, stagesExecutionInfo, originalExecutionId,
            isRetry, previousProcessedYaml, retryStagesIdentifier, uuidForSkipNode);

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
    ByteString gitSyncBranchContext = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal(pipelineEntity);
    if (gitSyncBranchContext != null) {
      builder.setGitSyncBranchContext(gitSyncBranchContext);
    }
    return builder.build();
  }

  private String getPipelineYamlAndValidate(String mergedRuntimeInputYaml, PipelineEntity pipelineEntity) {
    String pipelineYaml;
    if (EmptyPredicate.isEmpty(mergedRuntimeInputYaml)) {
      pipelineYaml = pipelineEntity.getYaml();
    } else {
      pipelineYaml =
          InputSetMergeHelper.mergeInputSetIntoPipeline(pipelineEntity.getYaml(), mergedRuntimeInputYaml, true);
    }
    pipelineYaml = InputSetSanitizer.trimValues(pipelineYaml);
    pmsYamlSchemaService.validateYamlSchema(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
        pipelineEntity.getProjectIdentifier(), pipelineYaml);
    pipelineRbacServiceImpl.extractAndValidateStaticallyReferredEntities(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(),
        pipelineYaml);
    return pipelineYaml;
  }

  private PlanExecutionMetadata obtainPlanExecutionMetadata(String mergedRuntimeInputYaml, String executionId,
      StagesExecutionInfo stagesExecutionInfo, String originalExecutionId, boolean isRetry,
      String previousProcessedYaml, List<String> retryStagesIdentifier, List<String> uuidForSkipNode) {
    String pipelineYaml = stagesExecutionInfo.getPipelineYamlToRun();
    PlanExecutionMetadata.Builder planExecutionMetadataBuilder =
        PlanExecutionMetadata.builder()
            .planExecutionId(executionId)
            .inputSetYaml(mergedRuntimeInputYaml)
            .yaml(pipelineYaml)
            .stagesExecutionMetadata(stagesExecutionInfo.toStagesExecutionMetadata());
    String currentProcessedYaml;
    try {
      currentProcessedYaml = YamlUtils.injectUuid(pipelineYaml);
    } catch (IOException e) {
      log.error("Unable to inject Uuids into pipeline Yaml. Yaml:\n" + pipelineYaml, e);
      throw new InvalidYamlException("Unable to inject Uuids into pipeline Yaml", e);
    }
    if (isRetry) {
      try {
        currentProcessedYaml = retryExecutionHelper.retryProcessedYaml(
            previousProcessedYaml, currentProcessedYaml, retryStagesIdentifier, uuidForSkipNode);
      } catch (IOException e) {
        log.error("Unable to get processed yaml. Previous Processed yaml:\n" + previousProcessedYaml, e);
        throw new InvalidYamlException("Unable to get processed yaml for retry.", e);
      }
    }
    planExecutionMetadataBuilder.processedYaml(currentProcessedYaml);

    if (EmptyPredicate.isNotEmpty(originalExecutionId)) {
      planExecutionMetadataBuilder = populateTriggerDataForRerun(originalExecutionId, planExecutionMetadataBuilder);
    }
    return planExecutionMetadataBuilder.build();
  }

  private PlanExecutionMetadata.Builder populateTriggerDataForRerun(
      String originalExecutionId, PlanExecutionMetadata.Builder planExecutionMetadataBuilder) {
    Optional<PlanExecutionMetadata> prevMetadataOptional =
        planExecutionMetadataService.findByPlanExecutionId(originalExecutionId);

    if (prevMetadataOptional.isPresent()) {
      PlanExecutionMetadata prevMetadata = prevMetadataOptional.get();
      return planExecutionMetadataBuilder.triggerPayload(prevMetadata.getTriggerPayload())
          .triggerJsonPayload(prevMetadata.getTriggerJsonPayload());
    } else {
      log.warn("No prev plan execution metadata found for plan execution id [" + originalExecutionId + "]");
    }
    return planExecutionMetadataBuilder;
  }

  public PlanExecution startExecution(String accountId, String orgIdentifier, String projectIdentifier,
      ExecutionMetadata executionMetadata, PlanExecutionMetadata planExecutionMetadata, boolean isRetry,
      List<String> uuidForSkipNode, String previousExecutionId) {
    long startTs = System.currentTimeMillis();
    PlanCreationBlobResponse resp;
    try {
      resp = planCreatorMergeService.createPlan(
          accountId, orgIdentifier, projectIdentifier, executionMetadata, planExecutionMetadata);
    } catch (IOException e) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(e)), e);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(e)), e);
    }
    Plan plan = PlanExecutionUtils.extractPlan(resp);
    ImmutableMap<String, String> abstractions = ImmutableMap.<String, String>builder()
                                                    .put(SetupAbstractionKeys.accountId, accountId)
                                                    .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
                                                    .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier)
                                                    .build();
    long endTs = System.currentTimeMillis();
    log.info("Time taken to complete plan: {}", endTs - startTs);

    if (isRetry) {
      retryExecutionHelper.transformPlan(plan, uuidForSkipNode, previousExecutionId);
      return orchestrationService.retryExecution(plan, abstractions, executionMetadata, planExecutionMetadata);
    }
    return orchestrationService.startExecution(plan, abstractions, executionMetadata, planExecutionMetadata);
  }

  public PlanExecution startExecutionV2(String accountId, String orgIdentifier, String projectIdentifier,
      ExecutionMetadata executionMetadata, PlanExecutionMetadata planExecutionMetadata, boolean isRetry,
      List<String> uuidForSkipNode) {
    long startTs = System.currentTimeMillis();
    String planCreationId = generateUuid();
    try {
      planCreatorMergeService.createPlanV2(
          accountId, orgIdentifier, projectIdentifier, planCreationId, executionMetadata, planExecutionMetadata);
    } catch (IOException e) {
      log.error(
          "Could not extract Pipeline Field from the processed yaml:\n" + planExecutionMetadata.getProcessedYaml(), e);
      throw new InvalidYamlException("Could not extract Pipeline Field from the yaml.", e);
    }

    ImmutableMap<String, String> abstractions = ImmutableMap.<String, String>builder()
                                                    .put(SetupAbstractionKeys.accountId, accountId)
                                                    .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
                                                    .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier)
                                                    .build();
    while (!planService.fetchPlanOptional(planCreationId).isPresent()) {
      Morpheus.sleep(Duration.ofMillis(100));
    }
    long endTs = System.currentTimeMillis();
    log.info("Time taken to complete plan: {}", endTs - startTs);
    Plan plan = planService.fetchPlan(planCreationId);
    if (!plan.isValid()) {
      PmsExceptionUtils.checkAndThrowPlanCreatorException(ImmutableList.of(plan.getErrorResponse()));
      return PlanExecution.builder().build();
    }
    if (isRetry) {
      return orchestrationService.retryExecution(plan, abstractions, executionMetadata, planExecutionMetadata);
    }
    return orchestrationService.startExecutionV2(
        planCreationId, abstractions, executionMetadata, planExecutionMetadata);
  }
}

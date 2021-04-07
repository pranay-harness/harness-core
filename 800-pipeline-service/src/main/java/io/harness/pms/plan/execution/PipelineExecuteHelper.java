package io.harness.pms.plan.execution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResourceClient;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.helpers.PrincipalInfoHelper;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.creation.PlanCreatorMergeService;
import io.harness.pms.preflight.PreFlightDTO;
import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.preflight.handler.AsyncPreFlightHandler;
import io.harness.pms.preflight.mapper.PreFlightMapper;
import io.harness.pms.rbac.validator.PipelineRbacService;
import io.harness.repositories.preflight.PreFlightRepository;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class PipelineExecuteHelper {
  private static final ExecutorService executorService = Executors.newFixedThreadPool(1);

  private final PMSPipelineService pmsPipelineService;
  private final OrchestrationService orchestrationService;
  private final PlanCreatorMergeService planCreatorMergeService;
  private final ValidateAndMergeHelper validateAndMergeHelper;
  private final ConnectorResourceClient connectorResourceClient;
  private final PipelineSetupUsageHelper pipelineSetupUsageHelper;
  private final PipelineRbacService pipelineRbacServiceImpl;
  private final PreFlightRepository preFlightRepository;

  public PlanExecution runPipelineWithInputSetPipelineYaml(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml,
      ExecutionTriggerInfo triggerInfo) throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.incrementRunSequence(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }

    String pipelineYaml;
    ExecutionMetadata.Builder executionMetadataBuilder = ExecutionMetadata.newBuilder()
                                                             .setExecutionUuid(generateUuid())
                                                             .setTriggerInfo(triggerInfo)
                                                             .setRunSequence(pipelineEntity.get().getRunSequence());

    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml, true);
      executionMetadataBuilder.setInputSetYaml(inputSetPipelineYaml);
    }
    pipelineRbacServiceImpl.validateStaticallyReferredEntitiesInYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);
    executionMetadataBuilder.setPipelineIdentifier(pipelineIdentifier);
    executionMetadataBuilder.setYaml(pipelineYaml);
    executionMetadataBuilder.setPrincipalInfo(PrincipalInfoHelper.getPrincipalInfoFromSecurityContext());

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
                                                             .setExecutionUuid(generateUuid())
                                                             .setTriggerInfo(triggerInfo)
                                                             .setRunSequence(pipelineEntity.get().getRunSequence());
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences);
    String pipelineYaml =
        MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), mergedRuntimeInputYaml, true);
    pipelineRbacServiceImpl.validateStaticallyReferredEntitiesInYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);

    executionMetadataBuilder.setPipelineIdentifier(pipelineIdentifier);
    executionMetadataBuilder.setInputSetYaml(mergedRuntimeInputYaml);
    executionMetadataBuilder.setYaml(pipelineYaml);
    executionMetadataBuilder.setPrincipalInfo(PrincipalInfoHelper.getPrincipalInfoFromSecurityContext());
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
            .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier);

    return orchestrationService.startExecution(plan, abstractionsBuilder.build(), executionMetadata);
  }

  public String startPreflightCheck(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml)
      throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }
    String pipelineYaml;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml, true);
    }
    pipelineRbacServiceImpl.validateStaticallyReferredEntitiesInYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);
    PreFlightEntity preFlightEntity =
        PreFlightMapper.toEmptyEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);
    PreFlightEntity preFlightEntitySaved = preFlightRepository.save(preFlightEntity);

    startAsyncHandling(preFlightEntitySaved);
    return preFlightEntitySaved.getUuid();
  }

  public PreFlightDTO getPreflightCheckResponse(String preflightCheckId) {
    Optional<PreFlightEntity> optionalPreFlightEntity = preFlightRepository.findById(preflightCheckId);
    if (!optionalPreFlightEntity.isPresent()) {
      throw new InvalidRequestException("Could not find pre flight check data corresponding to id:" + preflightCheckId);
    }
    PreFlightEntity preFlightEntity = optionalPreFlightEntity.get();
    return PreFlightMapper.toPreFlightDTO(preFlightEntity);
  }

  private void startAsyncHandling(PreFlightEntity entity) {
    executorService.submit(AsyncPreFlightHandler.builder()
                               .entity(entity)
                               .preFlightRepository(preFlightRepository)
                               .pipelineSetupUsageHelper(pipelineSetupUsageHelper)
                               .connectorResourceClient(connectorResourceClient)
                               .build());
  }
}

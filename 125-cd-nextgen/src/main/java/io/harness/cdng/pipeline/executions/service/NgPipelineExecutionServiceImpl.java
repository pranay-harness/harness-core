package io.harness.cdng.pipeline.executions.service;

import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.helpers.InputSetMergeHelper;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.cdng.pipeline.executions.TriggerType;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail.PipelineExecutionDetailBuilder;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.cdng.pipeline.executions.repositories.PipelineExecutionRepository;
import io.harness.cdng.pipeline.mappers.ExecutionToDtoMapper;
import io.harness.cdng.pipeline.mappers.NGPipelineExecutionDTOMapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.OrchestrationService;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.executions.beans.ExecutionGraph;
import io.harness.executions.mapper.ExecutionGraphMapper;
import io.harness.plan.Plan;
import io.harness.service.GraphGenerationService;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

@Singleton
public class NgPipelineExecutionServiceImpl implements NgPipelineExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;
  @Inject private PipelineExecutionRepository pipelineExecutionRepository;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private PipelineExecutionHelper pipelineExecutionHelper;
  @Inject private InputSetMergeHelper inputSetMergeHelper;

  @Override
  public PlanExecution runPipeline(
      String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user) {
    return startPipelinePlanExecution(accountId, orgId, projectId, pipelineYaml, user);
  }

  @Override
  public NGPipelineExecutionResponseDTO runPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      @NotNull String inputSetPipelineYaml, boolean useFQNIfErrorResponse, EmbeddedUser user) {
    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputSetPipelineYaml(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, inputSetPipelineYaml, false, useFQNIfErrorResponse);
    return getPipelineResponseDTO(accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse, user);
  }

  @Override
  public NGPipelineExecutionResponseDTO runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetReferences,
      boolean useFQNIfErrorResponse, EmbeddedUser user) {
    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, inputSetReferences, false, useFQNIfErrorResponse);

    return getPipelineResponseDTO(accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse, user);
  }

  @Override
  public Page<PipelineExecutionSummary> getExecutions(
      String accountId, String orgId, String projectId, Pageable pageable) {
    Criteria criteria = Criteria.where(PipelineExecutionSummaryKeys.accountIdentifier)
                            .is(accountId)
                            .and(PipelineExecutionSummaryKeys.orgIdentifier)
                            .is(orgId)
                            .and(PipelineExecutionSummaryKeys.projectIdentifier)
                            .is(projectId);

    return pipelineExecutionRepository.findAll(criteria, pageable);
  }

  @Override
  public PipelineExecutionSummary createPipelineExecutionSummary(
      String accountId, String orgId, String projectId, PlanExecution planExecution, NgPipeline ngPipeline) {
    Map<String, String> stageIdentifierToPlanNodeId = new HashMap<>();
    planExecution.getPlan()
        .getNodes()
        .stream()
        .filter(node -> Objects.equals(node.getGroup(), StepOutcomeGroup.STAGE.name()))
        .forEach(node -> stageIdentifierToPlanNodeId.put(node.getIdentifier(), node.getUuid()));
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgId)
                                                            .projectIdentifier(projectId)
                                                            .pipelineName(ngPipeline.getName())
                                                            .pipelineIdentifier(ngPipeline.getIdentifier())
                                                            .executionStatus(ExecutionStatus.RUNNING)
                                                            .triggeredBy(getEmbeddedUser())
                                                            .triggerType(TriggerType.MANUAL)
                                                            .planExecutionId(planExecution.getUuid())
                                                            .startedAt(planExecution.getStartTs())
                                                            .build();
    pipelineExecutionHelper.addStageSpecificDetailsToPipelineExecution(
        pipelineExecutionSummary, ngPipeline, stageIdentifierToPlanNodeId);
    return pipelineExecutionRepository.save(pipelineExecutionSummary);
  }

  @Override
  public PipelineExecutionDetail getPipelineExecutionDetail(@Nonnull String planExecutionId, String stageIdentifier) {
    PipelineExecutionDetailBuilder pipelineExecutionDetailBuilder = PipelineExecutionDetail.builder();

    if (EmptyPredicate.isNotEmpty(stageIdentifier)) {
      Optional<NodeExecution> stageNode = nodeExecutionService.getByNodeIdentifier(stageIdentifier, planExecutionId);
      if (!stageNode.isPresent()) {
        throw new InvalidRequestException(
            format("No Graph node found corresponding to identifier: [%s], planExecutionId: [%s]", stageIdentifier,
                planExecutionId));
      }
      OrchestrationGraphDTO orchestrationGraph = graphGenerationService.generatePartialOrchestrationGraph(
          stageNode.get().getNode().getUuid(), planExecutionId);
      @NonNull ExecutionGraph executionGraph = ExecutionGraphMapper.toExecutionGraph(orchestrationGraph);
      pipelineExecutionDetailBuilder.stageGraph(executionGraph);
    }

    return pipelineExecutionDetailBuilder
        .pipelineExecution(ExecutionToDtoMapper.writeExecutionDto(
            pipelineExecutionRepository.findByPlanExecutionId(planExecutionId).get()))
        .build();
  }

  @Override
  public PipelineExecutionSummary getByPlanExecutionId(
      String accountId, String orgId, String projectId, String planExecutionId) {
    return pipelineExecutionRepository
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
            accountId, orgId, projectId, planExecutionId)
        .orElseThrow(
            () -> new InvalidRequestException(format("Given plan execution id not found: %s", planExecutionId)));
  }

  @Override
  public PipelineExecutionSummary updateStatusForGivenNode(
      String accountId, String orgId, String projectId, String planExecutionId, NodeExecution nodeExecution) {
    PipelineExecutionSummary pipelineExecutionSummary =
        getByPlanExecutionId(accountId, orgId, projectId, planExecutionId);
    if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name())) {
      pipelineExecutionHelper.updateStageExecutionStatus(pipelineExecutionSummary, nodeExecution);
    } else if (nodeExecution.getNode().getGroup().equals(StepOutcomeGroup.PIPELINE.name())) {
      pipelineExecutionHelper.updatePipelineExecutionStatus(pipelineExecutionSummary, nodeExecution);
    }
    return pipelineExecutionRepository.save(pipelineExecutionSummary);
  }

  private PlanExecution startPipelinePlanExecution(
      String accountId, String orgIdentifier, String projectIdentifier, String finalPipelineYaml, EmbeddedUser user) {
    try {
      final NgPipeline cdPipeline;
      cdPipeline = YamlPipelineUtils.read(finalPipelineYaml, NgPipeline.class);
      Map<String, Object> contextAttributes = new HashMap<>();
      final Plan planForPipeline =
          executionPlanCreatorService.createPlanForPipeline(cdPipeline, accountId, contextAttributes);

      if (user == null) {
        user = getEmbeddedUser();
      }
      ImmutableMap.Builder<String, String> abstractionsBuilder =
          ImmutableMap.<String, String>builder()
              .put(SetupAbstractionKeys.accountId, accountId)
              .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
              .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier);
      if (user != null) {
        abstractionsBuilder.put(SetupAbstractionKeys.userId, user.getUuid())
            .put(SetupAbstractionKeys.userName, user.getName())
            .put(SetupAbstractionKeys.userEmail, user.getEmail());
      }
      return orchestrationService.startExecution(planForPipeline, abstractionsBuilder.build());
    } catch (IOException e) {
      throw new GeneralException("Error while de-serializing merged pipeline yaml", e);
    }
  }

  private NGPipelineExecutionResponseDTO getPipelineResponseDTO(String accountId, String orgIdentifier,
      String projectIdentifier, MergeInputSetResponse mergeInputSetResponse, EmbeddedUser user) {
    if (mergeInputSetResponse.isErrorResponse()) {
      return NGPipelineExecutionDTOMapper.toNGPipelineResponseDTO(null, mergeInputSetResponse);
    }
    PlanExecution planExecution = startPipelinePlanExecution(
        accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse.getPipelineYaml(), user);
    return NGPipelineExecutionDTOMapper.toNGPipelineResponseDTO(planExecution, mergeInputSetResponse);
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return EmbeddedUser.builder().build();
    }
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }
}

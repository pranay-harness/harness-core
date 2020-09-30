package io.harness.cdng.pipeline.executions.service;

import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.cdng.pipeline.executions.TriggerType;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail.PipelineExecutionDetailBuilder;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.cdng.pipeline.executions.repositories.PipelineExecutionRepository;
import io.harness.cdng.pipeline.mappers.ExecutionToDtoMapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraph;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;

@Singleton
public class NgPipelineExecutionServiceImpl implements NgPipelineExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;
  @Inject private PipelineExecutionRepository pipelineExecutionRepository;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private PipelineExecutionHelper pipelineExecutionHelper;

  @Override
  public PlanExecution triggerPipeline(
      String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user) {
    final NgPipeline ngPipeline;
    try {
      ngPipeline = YamlPipelineUtils.read(pipelineYaml, NgPipeline.class);
      Map<String, Object> contextAttributes = new HashMap<>();

      final Plan planForPipeline =
          executionPlanCreatorService.createPlanForPipeline(ngPipeline, accountId, contextAttributes);
      return orchestrationService.startExecution(planForPipeline,
          ImmutableMap.of(SetupAbstractionKeys.accountId, accountId, SetupAbstractionKeys.orgIdentifier, orgId,
              SetupAbstractionKeys.projectIdentifier, projectId));
    } catch (IOException e) {
      throw new GeneralException("error while de-serializing Yaml", e);
    }
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
      OrchestrationGraph orchestrationGraph = graphGenerationService.generatePartialOrchestrationGraph(
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

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return EmbeddedUser.builder().build();
    }
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }
}

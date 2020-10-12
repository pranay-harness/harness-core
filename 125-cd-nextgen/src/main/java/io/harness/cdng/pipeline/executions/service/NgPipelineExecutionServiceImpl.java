package io.harness.cdng.pipeline.executions.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.cdng.pipeline.executions.TriggerType;
import io.harness.cdng.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.ExecutionTriggerInfo;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail.PipelineExecutionDetailBuilder;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.cdng.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.cdng.pipeline.executions.repositories.PipelineExecutionRepository;
import io.harness.cdng.pipeline.mappers.ExecutionToDtoMapper;
import io.harness.cdng.pipeline.service.NGPipelineService;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executions.beans.ExecutionGraph;
import io.harness.executions.mapper.ExecutionGraphMapper;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.service.GraphGenerationService;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

@Singleton
public class NgPipelineExecutionServiceImpl implements NgPipelineExecutionService {
  private static final EmbeddedUser EMBEDDED_USER =
      EmbeddedUser.builder().uuid("lv0euRhKRCyiXWzS7pOg6g").email("admin@harness.io").name("Admin").build();

  @Inject private PipelineExecutionRepository pipelineExecutionRepository;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private PipelineExecutionHelper pipelineExecutionHelper;
  @Inject private NGPipelineService ngPipelineService;

  @Override
  public Page<PipelineExecutionSummary> getExecutions(String accountId, String orgId, String projectId,
      Pageable pageable, PipelineExecutionSummaryFilter pipelineExecutionSummaryFilter) {
    Criteria baseCriteria = Criteria.where(PipelineExecutionSummaryKeys.accountIdentifier)
                                .is(accountId)
                                .and(PipelineExecutionSummaryKeys.orgIdentifier)
                                .is(orgId)
                                .and(PipelineExecutionSummaryKeys.projectIdentifier)
                                .is(projectId);
    Criteria criteria = createPipelineExecutionSummaryFilterCriteria(baseCriteria, pipelineExecutionSummaryFilter);
    Page<PipelineExecutionSummary> pipelineExecutionSummaries = pipelineExecutionRepository.findAll(criteria, pageable);
    List<String> pipelineIdentifiers = pipelineExecutionSummaries.get()
                                           .map(PipelineExecutionSummary::getPipelineIdentifier)
                                           .collect(Collectors.toList());
    Map<String, String> pipelineIdentifierToNameMap =
        ngPipelineService.getPipelineIdentifierToName(accountId, orgId, projectId, pipelineIdentifiers);
    pipelineExecutionSummaries.get().forEach(pipelineExecutionSummary
        -> pipelineExecutionSummary.setPipelineName(
            pipelineIdentifierToNameMap.get(pipelineExecutionSummary.getPipelineIdentifier())));
    return pipelineExecutionSummaries;
  }

  private Criteria createPipelineExecutionSummaryFilterCriteria(
      Criteria criteria, PipelineExecutionSummaryFilter pipelineExecutionSummaryFilter) {
    if (pipelineExecutionSummaryFilter == null) {
      return criteria;
    }
    if (!EmptyPredicate.isEmpty(pipelineExecutionSummaryFilter.getExecutionStatuses())) {
      criteria.and(PipelineExecutionSummaryKeys.executionStatus)
          .in(pipelineExecutionSummaryFilter.getExecutionStatuses());
    }
    if (!isNull(pipelineExecutionSummaryFilter.getEnvironmentTypes())) {
      criteria.and(PipelineExecutionSummaryKeys.environmentTypes)
          .in(pipelineExecutionSummaryFilter.getEnvironmentTypes());
    }
    if (!isNull(pipelineExecutionSummaryFilter.getStartTime())) {
      criteria.and(PipelineExecutionSummaryKeys.startedAt).gte(pipelineExecutionSummaryFilter.getStartTime());
    }
    if (!isNull(pipelineExecutionSummaryFilter.getEndTime())) {
      criteria.and(PipelineExecutionSummaryKeys.endedAt).lte(pipelineExecutionSummaryFilter.getEndTime());
    }
    if (EmptyPredicate.isNotEmpty(pipelineExecutionSummaryFilter.getEnvIdentifiers())) {
      criteria.and(PipelineExecutionSummaryKeys.envIdentifiers).in(pipelineExecutionSummaryFilter.getEnvIdentifiers());
    }
    if (EmptyPredicate.isNotEmpty(pipelineExecutionSummaryFilter.getServiceIdentifiers())) {
      criteria.and(PipelineExecutionSummaryKeys.serviceIdentifiers)
          .in(pipelineExecutionSummaryFilter.getServiceIdentifiers());
    }
    if (EmptyPredicate.isNotEmpty(pipelineExecutionSummaryFilter.getSearchTerm())) {
      criteria.orOperator(Criteria.where(PipelineExecutionSummaryKeys.pipelineName)
                              .regex(pipelineExecutionSummaryFilter.getSearchTerm(), "i"),
          Criteria.where(PipelineExecutionSummaryKeys.tags).regex(pipelineExecutionSummaryFilter.getSearchTerm(), "i"));
    }
    return criteria;
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
    PipelineExecutionSummary pipelineExecutionSummary =
        PipelineExecutionSummary.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(orgId)
            .projectIdentifier(projectId)
            .pipelineName(ngPipeline.getName())
            .pipelineIdentifier(ngPipeline.getIdentifier())
            .executionStatus(ExecutionStatus.RUNNING)
            .triggerInfo(
                ExecutionTriggerInfo.builder().triggerType(TriggerType.MANUAL).triggeredBy(EMBEDDED_USER).build())
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
      OrchestrationGraphDTO orchestrationGraph =
          graphGenerationService.generatePartialOrchestrationGraphFromSetupNodeId(
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

  @Override
  public PipelineExecutionSummary addServiceInformationToPipelineExecutionNode(String accountId, String orgId,
      String projectId, String planExecutionId, String nodeExecutionId, ServiceOutcome serviceOutcome) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    PipelineExecutionSummary pipelineExecutionSummary =
        getByPlanExecutionId(accountId, orgId, projectId, planExecutionId);
    CDStageExecutionSummary stageExecutionSummaryWrapper =
        pipelineExecutionHelper.findStageExecutionSummaryByNodeExecutionId(
            pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), nodeExecution.getParentId());
    ServiceExecutionSummary serviceExecutionSummary =
        ServiceExecutionSummary.builder()
            .identifier(serviceOutcome.getIdentifier())
            .displayName(serviceOutcome.getDisplayName())
            .deploymentType(serviceOutcome.getDeploymentType())
            .artifacts(pipelineExecutionHelper.mapArtifactsOutcomeToSummary(serviceOutcome))
            .build();
    stageExecutionSummaryWrapper.setServiceExecutionSummary(serviceExecutionSummary);
    stageExecutionSummaryWrapper.setServiceIdentifier(serviceExecutionSummary.getIdentifier());
    pipelineExecutionSummary.addServiceIdentifier(serviceExecutionSummary.getIdentifier());
    pipelineExecutionSummary.addServiceDefinitionType(serviceExecutionSummary.getDeploymentType());
    pipelineExecutionRepository.save(pipelineExecutionSummary);
    return pipelineExecutionSummary;
  }

  @Override
  public List<ExecutionStatus> getExecutionStatuses() {
    return Arrays.asList(ExecutionStatus.values());
  }

  @Override
  public Map<ExecutionNodeType, String> getStepTypeToYamlTypeMapping() {
    return Arrays.stream(ExecutionNodeType.values())
        .collect(Collectors.toMap(executionStepType -> executionStepType, ExecutionNodeType::getYamlType, (a, b) -> b));
  }
}

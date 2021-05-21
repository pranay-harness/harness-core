package io.harness.cdng.pipeline.executions.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.contracts.plan.TriggerType.MANUAL;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.cdng.pipeline.executions.PipelineExecutionHelper.StageIndex;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.engine.OrchestrationService;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.interrupts.Interrupt;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionInterruptDTO;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.pms.contracts.advisers.IssuedBy;
import io.harness.pms.contracts.advisers.ManualIssuer;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.repositories.pipeline.PipelineExecutionRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class NgPipelineExecutionServiceImpl implements NgPipelineExecutionService {
  private static final TriggeredBy EMBEDDED_USER = TriggeredBy.newBuilder()
                                                       .setUuid("lv0euRhKRCyiXWzS7pOg6g")
                                                       .putExtraInfo("email", "admin@harness.io")
                                                       .setIdentifier("Admin")
                                                       .build();

  @Inject private PipelineExecutionRepository pipelineExecutionRepository;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PipelineExecutionHelper pipelineExecutionHelper;
  @Inject private NGPipelineService ngPipelineService;
  @Inject private OrchestrationService orchestrationService;

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
    if (!isEmpty(pipelineExecutionSummaryFilter.getExecutionStatuses())) {
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
    if (isNotEmpty(pipelineExecutionSummaryFilter.getEnvIdentifiers())) {
      criteria.and(PipelineExecutionSummaryKeys.envIdentifiers).in(pipelineExecutionSummaryFilter.getEnvIdentifiers());
    }
    if (isNotEmpty(pipelineExecutionSummaryFilter.getPipelineIdentifiers())) {
      criteria.and(PipelineExecutionSummaryKeys.pipelineIdentifier)
          .in(pipelineExecutionSummaryFilter.getPipelineIdentifiers());
    }
    if (isNotEmpty(pipelineExecutionSummaryFilter.getServiceIdentifiers())) {
      criteria.and(PipelineExecutionSummaryKeys.serviceIdentifiers)
          .in(pipelineExecutionSummaryFilter.getServiceIdentifiers());
    }
    if (isNotEmpty(pipelineExecutionSummaryFilter.getSearchTerm())) {
      criteria.orOperator(Criteria.where(PipelineExecutionSummaryKeys.pipelineName)
                              .regex(pipelineExecutionSummaryFilter.getSearchTerm(), "i"),
          Criteria.where(PipelineExecutionSummaryKeys.tags).regex(pipelineExecutionSummaryFilter.getSearchTerm(), "i"));
    }
    return criteria;
  }

  @Override
  public PipelineExecutionSummary createPipelineExecutionSummary(String accountId, String orgId, String projectId,
      PlanExecution planExecution, CDPipelineSetupParameters cdPipelineSetupParameters) {
    NgPipeline ngPipeline = cdPipelineSetupParameters.getNgPipeline();
    String inputSetYaml = cdPipelineSetupParameters.getInputSetPipelineYaml();
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
            .triggerInfo(ExecutionTriggerInfo.newBuilder().setTriggerType(MANUAL).setTriggeredBy(EMBEDDED_USER).build())
            .planExecutionId(planExecution.getUuid())
            .startedAt(planExecution.getStartTs())
            .inputSetYaml(inputSetYaml)
            .build();
    pipelineExecutionHelper.addStageSpecificDetailsToPipelineExecution(
        pipelineExecutionSummary, ngPipeline, stageIdentifierToPlanNodeId);
    return pipelineExecutionRepository.save(pipelineExecutionSummary);
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
  public void updateStatusForGivenNode(
      String accountId, String orgId, String projectId, String planExecutionId, NodeExecution nodeExecution) {
    if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name())) {
      PipelineExecutionSummary pipelineExecutionSummary =
          getByPlanExecutionId(accountId, orgId, projectId, planExecutionId);
      StageIndex stageIndex = pipelineExecutionHelper.findStageIndexByPlanNodeId(
          pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), nodeExecution.getNode().getUuid());
      if (stageIndex == null) {
        throw new UnexpectedException("No Stage Found for plan node Id: " + nodeExecution.getNode().getUuid());
      }
      pipelineExecutionRepository.findAndUpdate(
          planExecutionId, pipelineExecutionHelper.getCDStageExecutionSummaryStatusUpdate(stageIndex, nodeExecution));
    } else if (nodeExecution.getNode().getGroup().equals(StepOutcomeGroup.PIPELINE.name())) {
      PipelineExecutionSummary pipelineExecutionSummary =
          getByPlanExecutionId(accountId, orgId, projectId, planExecutionId);
      pipelineExecutionHelper.updatePipelineExecutionStatus(pipelineExecutionSummary, nodeExecution);
      pipelineExecutionRepository.save(pipelineExecutionSummary);
    }
  }

  @Override
  public PipelineExecutionSummary addServiceInformationToPipelineExecutionNode(String accountId, String orgId,
      String projectId, String planExecutionId, String nodeExecutionId, ServiceOutcome serviceOutcome) {
    NodeExecution stageNodeExecution = getStageNodeExecution(nodeExecutionId);
    PipelineExecutionSummary pipelineExecutionSummary =
        getByPlanExecutionId(accountId, orgId, projectId, planExecutionId);
    StageIndex stageIndex = pipelineExecutionHelper.findStageIndexByNodeExecutionId(
        pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), stageNodeExecution.getUuid());
    if (stageIndex == null) {
      throw new UnexpectedException("No Stage Found for plan node Id: " + stageNodeExecution.getNode().getUuid());
    }
    ServiceExecutionSummary serviceExecutionSummary =
        ServiceExecutionSummary.builder()
            .identifier(serviceOutcome.getIdentifier())
            .displayName(serviceOutcome.getName())
            .deploymentType(serviceOutcome.getType())
            .artifacts(pipelineExecutionHelper.mapArtifactsOutcomeToSummary(serviceOutcome))
            .build();
    pipelineExecutionRepository.findAndUpdate(planExecutionId,
        pipelineExecutionHelper.getCDStageExecutionSummaryServiceUpdate(stageIndex, serviceExecutionSummary));
    return pipelineExecutionSummary;
  }

  @Override
  public PipelineExecutionSummary addEnvironmentInformationToPipelineExecutionNode(String accountId, String orgId,
      String projectId, String planExecutionId, String nodeExecutionId, EnvironmentOutcome environmentOutcome) {
    NodeExecution stageNodeExecution = getStageNodeExecution(nodeExecutionId);
    PipelineExecutionSummary pipelineExecutionSummary =
        getByPlanExecutionId(accountId, orgId, projectId, planExecutionId);
    StageIndex stageIndex = pipelineExecutionHelper.findStageIndexByNodeExecutionId(
        pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), stageNodeExecution.getUuid());
    if (stageIndex == null) {
      throw new UnexpectedException("No Stage Found for plan node Id: " + stageNodeExecution.getNode().getUuid());
    }
    pipelineExecutionRepository.findAndUpdate(planExecutionId,
        pipelineExecutionHelper.getCDStageExecutionSummaryEnvironmentUpdate(stageIndex, environmentOutcome));
    return pipelineExecutionSummary;
  }

  @Override
  public List<ExecutionStatus> getExecutionStatuses() {
    return Arrays.asList(ExecutionStatus.values());
  }

  @Override
  public PipelineExecutionInterruptDTO registerInterrupt(
      PipelineExecutionInterruptType executionInterruptType, String planExecutionId) {
    // TODO(sahil): we need to clean these apis plus these extra pojos including PipelineExecutionInterruptType
    InterruptConfig interruptConfig =
        InterruptConfig.newBuilder()
            .setIssuedBy(IssuedBy.newBuilder().setManualIssuer(ManualIssuer.newBuilder().build()).build())
            .build();
    InterruptPackage interruptPackage = InterruptPackage.builder()
                                            .interruptType(executionInterruptType.getExecutionInterruptType())
                                            .planExecutionId(planExecutionId)
                                            .interruptConfig(interruptConfig)
                                            .build();
    Interrupt interrupt = orchestrationService.registerInterrupt(interruptPackage);
    return PipelineExecutionInterruptDTO.builder()
        .id(interrupt.getUuid())
        .planExecutionId(interrupt.getPlanExecutionId())
        .type(executionInterruptType)
        .build();
  }

  @Override
  public Map<ExecutionNodeType, String> getStepTypeToYamlTypeMapping() {
    return Arrays.stream(ExecutionNodeType.values())
        .collect(Collectors.toMap(executionStepType -> executionStepType, ExecutionNodeType::getYamlType, (a, b) -> b));
  }

  private NodeExecution getStageNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name())) {
      return nodeExecution;
    }
    return getStageNodeExecution(nodeExecution.getParentId());
  }
}

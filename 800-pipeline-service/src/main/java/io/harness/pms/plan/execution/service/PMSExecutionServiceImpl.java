package io.harness.pms.plan.execution.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.plan.TriggerType.MANUAL;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.OrchestrationService;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.interrupts.Interrupt;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.pms.contracts.advisers.IssuedBy;
import io.harness.pms.contracts.advisers.ManualIssuer;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.filter.utils.ModuleInfoFilterUtils;
import io.harness.pms.helpers.TriggeredByHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.plan.execution.PlanExecutionInterruptType;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys;
import io.harness.pms.plan.execution.beans.dto.InterruptDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;
import io.harness.pms.utils.PmsConstants;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.serializer.JsonUtils;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class PMSExecutionServiceImpl implements PMSExecutionService {
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRespository;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OrchestrationService orchestrationService;
  @Inject private FilterService filterService;
  @Inject private TriggeredByHelper triggeredByHelper;

  @Override
  public Criteria formCriteria(String accountId, String orgId, String projectId, String pipelineIdentifier,
      String filterIdentifier, PipelineExecutionFilterPropertiesDTO filterProperties, String moduleName,
      String searchTerm, ExecutionStatus status, boolean myDeployments, boolean pipelineDeleted) {
    Criteria criteria = new Criteria();
    if (EmptyPredicate.isNotEmpty(accountId)) {
      criteria.and(PlanExecutionSummaryKeys.accountId).is(accountId);
    }
    if (EmptyPredicate.isNotEmpty(orgId)) {
      criteria.and(PlanExecutionSummaryKeys.orgIdentifier).is(orgId);
    }
    if (EmptyPredicate.isNotEmpty(projectId)) {
      criteria.and(PlanExecutionSummaryKeys.projectIdentifier).is(projectId);
    }
    if (EmptyPredicate.isNotEmpty(pipelineIdentifier)) {
      criteria.and(PlanExecutionSummaryKeys.pipelineIdentifier).is(pipelineIdentifier);
    }
    if (status != null) {
      criteria.and(PlanExecutionSummaryKeys.status).is(status);
    }
    criteria.and(PlanExecutionSummaryKeys.pipelineDeleted).ne(!pipelineDeleted);

    if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    } else if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties == null) {
      populatePipelineFilterUsingIdentifier(criteria, accountId, orgId, projectId, filterIdentifier);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      populatePipelineFilter(criteria, filterProperties);
    }

    if (myDeployments) {
      criteria.and(PlanExecutionSummaryKeys.executionTriggerInfo)
          .is(ExecutionTriggerInfo.newBuilder()
                  .setTriggerType(MANUAL)
                  .setTriggeredBy(triggeredByHelper.getFromSecurityContext())
                  .build());
    }

    if (EmptyPredicate.isNotEmpty(moduleName)) {
      // Check for pipeline with no filters also - empty pipeline or pipelines with only approval stage
      criteria.orOperator(Criteria.where(PlanExecutionSummaryKeys.modules).is(Collections.emptyList()),
          Criteria.where(PlanExecutionSummaryKeys.modules)
              .is(Collections.singletonList(PmsConstants.INTERNAL_SERVICE_NAME)),
          Criteria.where(PlanExecutionSummaryKeys.modules).in(moduleName),
          Criteria.where(String.format("moduleInfo.%s", moduleName)).exists(true));
    }
    if (EmptyPredicate.isNotEmpty(searchTerm)) {
      Criteria searchCriteria =
          new Criteria().orOperator(where(PlanExecutionSummaryKeys.pipelineIdentifier)
                                        .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
              where(PlanExecutionSummaryKeys.name)
                  .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
              where(PlanExecutionSummaryKeys.tags + "." + NGTagKeys.key)
                  .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
              where(PlanExecutionSummaryKeys.tags + "." + NGTagKeys.value)
                  .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
      criteria.andOperator(searchCriteria);
    }
    return criteria;
  }

  private void populatePipelineFilterUsingIdentifier(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String filterIdentifier) {
    FilterDTO pipelineFilterDTO = this.filterService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.PIPELINEEXECUTION);
    if (pipelineFilterDTO == null) {
      throw new InvalidRequestException("Could not find a pipeline filter with the identifier ");
    }
    this.populatePipelineFilter(
        criteria, (PipelineExecutionFilterPropertiesDTO) pipelineFilterDTO.getFilterProperties());
  }

  private void populatePipelineFilter(Criteria criteria, @NotNull PipelineExecutionFilterPropertiesDTO piplineFilter) {
    if (EmptyPredicate.isNotEmpty(piplineFilter.getPipelineName())) {
      criteria.and(PlanExecutionSummaryKeys.name).is(piplineFilter.getPipelineName());
    }
    if (EmptyPredicate.isNotEmpty(piplineFilter.getStatus())) {
      criteria.and(PlanExecutionSummaryKeys.status).in(piplineFilter.getStatus());
    }
    if (piplineFilter.getModuleProperties() != null) {
      ModuleInfoFilterUtils.processNode(
          JsonUtils.readTree(piplineFilter.getModuleProperties().toJson()), "moduleInfo", criteria);
    }
  }

  @Override
  public String getInputSetYaml(
      String accountId, String orgId, String projectId, String planExecutionId, boolean pipelineDeleted) {
    Optional<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntityOptional =
        pmsExecutionSummaryRespository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
                accountId, orgId, projectId, planExecutionId, !pipelineDeleted);
    if (pipelineExecutionSummaryEntityOptional.isPresent()) {
      return pipelineExecutionSummaryEntityOptional.get().getInputSetYaml();
    }
    throw new InvalidRequestException(
        "Invalid request : Input Set did not exist or pipeline execution has been deleted");
  }

  @Override
  public PipelineExecutionSummaryEntity getPipelineExecutionSummaryEntity(
      String accountId, String orgId, String projectId, String planExecutionId, boolean pipelineDeleted) {
    Optional<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntityOptional =
        pmsExecutionSummaryRespository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
                accountId, orgId, projectId, planExecutionId, !pipelineDeleted);
    if (pipelineExecutionSummaryEntityOptional.isPresent()) {
      return pipelineExecutionSummaryEntityOptional.get();
    }
    throw new InvalidRequestException(
        "Plan Execution Summary does not exist or has been deleted for given planExecutionId");
  }

  @Override
  public Page<PipelineExecutionSummaryEntity> getPipelineExecutionSummaryEntity(Criteria criteria, Pageable pageable) {
    return pmsExecutionSummaryRespository.findAll(criteria, pageable);
  }

  @Override
  public OrchestrationGraphDTO getOrchestrationGraph(String stageNodeId, String planExecutionId) {
    if (EmptyPredicate.isEmpty(stageNodeId)) {
      return graphGenerationService.generateOrchestrationGraphV2(planExecutionId);
    }
    return graphGenerationService.generatePartialOrchestrationGraphFromSetupNodeId(stageNodeId, planExecutionId);
  }

  @Override
  public InterruptDTO registerInterrupt(
      PlanExecutionInterruptType executionInterruptType, String planExecutionId, String nodeExecutionId) {
    InterruptPackage interruptPackage =
        InterruptPackage.builder()
            .interruptType(executionInterruptType.getExecutionInterruptType())
            .planExecutionId(planExecutionId)
            .nodeExecutionId(nodeExecutionId)
            .interruptConfig(
                InterruptConfig.newBuilder()
                    .setIssuedBy(IssuedBy.newBuilder().setManualIssuer(ManualIssuer.newBuilder().build()).build())
                    .build())
            .metadata(getMetadata(executionInterruptType))
            .build();
    Interrupt interrupt = orchestrationService.registerInterrupt(interruptPackage);
    return InterruptDTO.builder()
        .id(interrupt.getUuid())
        .planExecutionId(interrupt.getPlanExecutionId())
        .type(executionInterruptType)
        .build();
  }

  private Map<String, String> getMetadata(PlanExecutionInterruptType planExecutionInterruptType) {
    if (planExecutionInterruptType == PlanExecutionInterruptType.STAGEROLLBACK
        || planExecutionInterruptType == PlanExecutionInterruptType.STEPGROUPROLLBACK) {
      return Collections.singletonMap("ROLLBACK", planExecutionInterruptType.getDisplayName());
    }
    return Collections.emptyMap();
  }

  @Override
  public void deleteExecutionsOnPipelineDeletion(PipelineEntity pipelineEntity) {
    Criteria criteria = new Criteria();
    criteria.and(PlanExecutionSummaryKeys.accountId)
        .is(pipelineEntity.getAccountId())
        .and(PlanExecutionSummaryKeys.orgIdentifier)
        .is(pipelineEntity.getOrgIdentifier())
        .and(PlanExecutionSummaryKeys.projectIdentifier)
        .is(pipelineEntity.getProjectIdentifier())
        .and(PlanExecutionSummaryKeys.pipelineIdentifier)
        .is(pipelineEntity.getIdentifier());
    Query query = new Query(criteria);

    Update update = new Update();
    update.set(PlanExecutionSummaryKeys.pipelineDeleted, Boolean.TRUE);

    UpdateResult updateResult = pmsExecutionSummaryRespository.deleteAllExecutionsWhenPipelineDeleted(query, update);
    if (!updateResult.wasAcknowledged()) {
      throw new InvalidRequestException(format(
          "Executions for Pipeline [%s] under Project[%s], Organization [%s] couldn't be deleted.",
          pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
    }
  }
}

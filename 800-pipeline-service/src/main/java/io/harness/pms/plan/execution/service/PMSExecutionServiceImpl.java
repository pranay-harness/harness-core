package io.harness.pms.plan.execution.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.encryption.ScopeHelper;
import io.harness.engine.OrchestrationService;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.interrupts.Interrupt;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.plan.execution.PlanExecutionInterruptType;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.InterruptDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
public class PMSExecutionServiceImpl implements PMSExecutionService {
  String inputSetYaml = "inputSet:\n"
      + "  identifier: identifier\n"
      + "  description: second input set for unknown pipeline\n"
      + "  tags:\n"
      + "    company: harness\n"
      + "  pipeline:\n"
      + "    identifier: pipeline_identifier\n"
      + "    stages:\n"
      + "      - stage:\n"
      + "          identifier: qa_again\n"
      + "          type: Deployment\n"
      + "          spec:\n"
      + "            execution:\n"
      + "              steps:\n"
      + "                - parallel:\n"
      + "                    - step:\n"
      + "                        identifier: rolloutDeployment\n"
      + "                        type: K8sRollingDeploy\n"
      + "                        spec:\n"
      + "                          timeout: 60000\n"
      + "                          skipDryRun: false";
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRespository;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OrchestrationService orchestrationService;
  @Inject private FilterService filterService;

  @Override
  public Criteria formCriteria(String accountId, String orgId, String projectId, String pipelineIdentifier,
      String filterIdentifier, PipelineExecutionFilterPropertiesDTO filterProperties) {
    Criteria criteria = new Criteria();
    if (EmptyPredicate.isNotEmpty(accountId)) {
      criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.accountId).is(accountId);
    }
    if (EmptyPredicate.isNotEmpty(orgId)) {
      criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.orgIdentifier).is(orgId);
    }
    if (EmptyPredicate.isNotEmpty(projectId)) {
      criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.projectIdentifier).is(projectId);
    }
    if (EmptyPredicate.isNotEmpty(pipelineIdentifier)) {
      criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.pipelineIdentifier).is(pipelineIdentifier);
    }

    if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties != null) {
      throw new InvalidRequestException("Can not apply both filter properties and saved filter together");
    } else if (EmptyPredicate.isNotEmpty(filterIdentifier) && filterProperties == null) {
      populatePipelineFilter(criteria, accountId, orgId, projectId, filterIdentifier);
    } else if (EmptyPredicate.isEmpty(filterIdentifier) && filterProperties != null) {
      populatePipelineFilter(criteria, filterProperties);
    } else {
      return criteria;
    }
    return criteria;
  }

  private void populatePipelineFilter(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String filterIdentifier) {
    FilterDTO pipelineFilterDTO = this.filterService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.PIPELINE);
    if (pipelineFilterDTO == null) {
      throw new InvalidRequestException("Could not find a pipeline filter with the identifier ");
    } else {
      this.populatePipelineFilter(
          criteria, (PipelineExecutionFilterPropertiesDTO) pipelineFilterDTO.getFilterProperties());
    }
  }

  private void populatePipelineFilter(Criteria criteria, @NotNull PipelineExecutionFilterPropertiesDTO piplineFilter) {
    if (EmptyPredicate.isNotEmpty(piplineFilter.getPipelineName())) {
      criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.name).is(piplineFilter.getPipelineName());
    }
    if (piplineFilter.getStatus() != null) {
      criteria.and(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.status).is(piplineFilter.getStatus());
    }
  }

  @Override
  public String getInputsetYaml(String accountId, String orgId, String projectId, String planExecutionId) {
    Optional<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntityOptional =
        pmsExecutionSummaryRespository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
            accountId, orgId, projectId, planExecutionId);
    if (pipelineExecutionSummaryEntityOptional.isPresent()) {
      return pipelineExecutionSummaryEntityOptional.get().getInputSetYaml();
    }
    throw InvalidRequestException.builder().message("Invalid request").build();
  }

  @Override
  public PipelineExecutionSummaryEntity getPipelineExecutionSummaryEntity(
      String accountId, String orgId, String projectId, String planExecutionId) {
    Optional<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntityOptional =
        pmsExecutionSummaryRespository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
            accountId, orgId, projectId, planExecutionId);
    if (pipelineExecutionSummaryEntityOptional.isPresent()) {
      return pipelineExecutionSummaryEntityOptional.get();
    }
    throw new InvalidRequestException("Plan Execution Summary does not exist with given planExecutionId");
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
  public InterruptDTO registerInterrupt(PlanExecutionInterruptType executionInterruptType, String planExecutionId) {
    InterruptPackage interruptPackage = InterruptPackage.builder()
                                            .interruptType(executionInterruptType.getExecutionInterruptType())
                                            .planExecutionId(planExecutionId)
                                            .build();
    Interrupt interrupt = orchestrationService.registerInterrupt(interruptPackage);
    return InterruptDTO.builder()
        .id(interrupt.getUuid())
        .planExecutionId(interrupt.getPlanExecutionId())
        .type(executionInterruptType)
        .build();
  }
}

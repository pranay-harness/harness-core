package io.harness.pms.exception.service;

import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

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
}

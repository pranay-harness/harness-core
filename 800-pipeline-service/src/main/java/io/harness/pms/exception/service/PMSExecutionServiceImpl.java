package io.harness.pms.exception.service;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

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
  @Override
  public String getInputsetYaml(String planExecutionId) {
    // Returning constant string for now
    if (!inputSetYaml.isEmpty())
      return inputSetYaml;
    Optional<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntityOptional =
        pmsExecutionSummaryRespository.findByPlanExecutionId(planExecutionId);
    if (pipelineExecutionSummaryEntityOptional.isPresent()) {
      return pipelineExecutionSummaryEntityOptional.get().getInputSetYaml();
    }
    throw InvalidRequestException.builder().message("Invalid request").build();
  }
}

package io.harness.cdng.pipeline.service;
import io.harness.beans.EmbeddedUser;
import io.harness.execution.PlanExecution;

public interface NgPipelineExecutionService {
  PlanExecution triggerPipeline(
      String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user);
}

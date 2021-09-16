package io.harness.pms.opa.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.opa.PipelineOpaEvaluationContext;

import java.io.IOException;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PMSOpaService {
  PipelineOpaEvaluationContext getPipelineContext(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml,
      @NotNull String action) throws IOException;

  PipelineOpaEvaluationContext getPipelineContextFromExecution(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String planExecutionId, @NotNull String action) throws IOException;
}

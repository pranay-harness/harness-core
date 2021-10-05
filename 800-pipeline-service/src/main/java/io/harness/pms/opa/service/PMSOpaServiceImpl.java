package io.harness.pms.opa.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.opaclient.OpaUtils;
import io.harness.pms.helpers.CurrentUserHelper;
import io.harness.pms.merger.helpers.InputSetMergeHelper;
import io.harness.pms.opa.PipelineOpaEvaluationContext;
import io.harness.pms.opa.PipelineOpaEvaluationContext.PipelineOpaEvaluationContextBuilder;
import io.harness.pms.opa.UserOpaEvaluationContext;
import io.harness.pms.opa.UserOpaEvaluationContext.UserOpaEvaluationContextBuilder;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.plan.execution.service.PipelineExecutionSummaryService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PMSOpaServiceImpl implements PMSOpaService {
  private final PMSPipelineService pmsPipelineService;
  private final PMSExecutionService pmsExecutionService;
  private final CurrentUserHelper currentUserHelper;
  private final PipelineExecutionSummaryService pipelineExecutionSummaryService;

  @Override
  public PipelineOpaEvaluationContext getPipelineContext(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml,
      @NotNull String action) throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }
    String pipelineYaml;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml =
          InputSetMergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml, false);
    }
    PipelineOpaEvaluationContextBuilder pipelineBuilder =
        PipelineOpaEvaluationContext.builder().pipeline(OpaUtils.extractObjectFromYamlString(pipelineYaml, "pipeline"));
    EmbeddedUser embeddedUser = currentUserHelper.getFromSecurityContext();
    UserOpaEvaluationContextBuilder userBuilder =
        UserOpaEvaluationContext.builder().email(embeddedUser.getEmail()).name(embeddedUser.getName());

    pipelineBuilder.user(userBuilder.build());
    pipelineBuilder.action(action);
    return pipelineBuilder.build();
  }

  @Override
  public PipelineOpaEvaluationContext getPipelineContextFromExecution(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String planExecutionId,
      @NotNull String action) throws IOException {
    PipelineExecutionSummaryEntity executionSummaryEntity =
        pipelineExecutionSummaryService.getPipelineExecutionSummaryEntity(
            accountId, orgIdentifier, projectIdentifier, planExecutionId, false);
    return getPipelineContext(accountId, orgIdentifier, projectIdentifier,
        executionSummaryEntity.getPipelineIdentifier(), executionSummaryEntity.getInputSetYaml(), action);
  }
}

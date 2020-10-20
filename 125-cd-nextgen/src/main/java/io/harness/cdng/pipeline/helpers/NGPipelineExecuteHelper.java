package io.harness.cdng.pipeline.helpers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.mappers.NGPipelineExecutionDTOMapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.inputset.helpers.InputSetMergeHelper;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.plan.Plan;
import io.harness.walktree.visitor.response.VisitorErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.core.StageElement;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@Singleton
public class NGPipelineExecuteHelper {
  @Inject private InputSetMergeHelper inputSetMergeHelper;
  @Inject private OrchestrationService orchestrationService;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;

  public NGPipelineExecutionResponseDTO runPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String inputSetPipelineYaml, boolean useFQNIfErrorResponse, EmbeddedUser user) {
    MergeInputSetResponse mergeInputSetResponse;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      NgPipeline pipeline = inputSetMergeHelper.getOriginalOrTemplatePipeline(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      mergeInputSetResponse = MergeInputSetResponse.builder().mergedPipeline(pipeline).build();
    } else {
      mergeInputSetResponse = inputSetMergeHelper.getMergePipelineYamlFromInputSetPipelineYaml(accountId, orgIdentifier,
          projectIdentifier, pipelineIdentifier, inputSetPipelineYaml, false, useFQNIfErrorResponse);
    }
    return getPipelineResponseDTO(accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse, user);
  }

  public NGPipelineExecutionResponseDTO runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetReferences,
      boolean useFQNIfErrorResponse, EmbeddedUser user) {
    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, inputSetReferences, false, useFQNIfErrorResponse);

    return getPipelineResponseDTO(accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse, user);
  }

  /**
   * Todo: Proper implementation will be after merging validation framework.
   * @param pipelineId
   * @param accountId
   * @param orgId
   * @param projectId
   * @return
   */
  public Optional<CDPipelineValidationInfo> validatePipeline(
      String pipelineId, String accountId, String orgId, String projectId) {
    Map<String, VisitorErrorResponseWrapper> uuidToErrorResponse = new HashMap<>();
    VisitorErrorResponse errorResponse =
        VisitorErrorResponse.errorBuilder().fieldName("identifier").message("cannot be null").build();
    uuidToErrorResponse.put(
        "pipeline.identifier", VisitorErrorResponseWrapper.builder().errors(Lists.newArrayList(errorResponse)).build());
    uuidToErrorResponse.put("pipeline.stage.identifier",
        VisitorErrorResponseWrapper.builder().errors(Lists.newArrayList(errorResponse)).build());
    NgPipeline ngPipeline =
        NgPipeline.builder()
            .identifier("pipeline.identifier")
            .name("dummyPipeline")
            .stage(StageElement.builder().name("dummyStage").identifier("pipeline.stage.identifier").build())
            .build();
    CDPipelineValidationInfo cdPipelineValidationInfo = CDPipelineValidationInfo.builder()
                                                            .uuidToValidationErrors(uuidToErrorResponse)
                                                            .ngPipeline(ngPipeline)
                                                            .isError(true)
                                                            .build();
    return Optional.of(cdPipelineValidationInfo);
  }

  private NGPipelineExecutionResponseDTO getPipelineResponseDTO(String accountId, String orgIdentifier,
      String projectIdentifier, MergeInputSetResponse mergeInputSetResponse, EmbeddedUser user) {
    if (mergeInputSetResponse.isErrorResponse()) {
      return NGPipelineExecutionDTOMapper.toNGPipelineResponseDTO(null, mergeInputSetResponse);
    }
    PlanExecution planExecution = startPipelinePlanExecution(
        accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse.getMergedPipeline(), user);
    return NGPipelineExecutionDTOMapper.toNGPipelineResponseDTO(planExecution, mergeInputSetResponse);
  }

  private PlanExecution startPipelinePlanExecution(
      String accountId, String orgIdentifier, String projectIdentifier, NgPipeline finalPipeline, EmbeddedUser user) {
    Map<String, Object> contextAttributes = new HashMap<>();
    final Plan planForPipeline =
        executionPlanCreatorService.createPlanForPipeline(finalPipeline, accountId, contextAttributes);

    if (user == null) {
      user = getEmbeddedUser();
    }
    ImmutableMap.Builder<String, String> abstractionsBuilder =
        ImmutableMap.<String, String>builder()
            .put(SetupAbstractionKeys.accountId, accountId)
            .put(SetupAbstractionKeys.orgIdentifier, orgIdentifier)
            .put(SetupAbstractionKeys.projectIdentifier, projectIdentifier);
    if (user != null) {
      abstractionsBuilder.put(SetupAbstractionKeys.userId, user.getUuid())
          .put(SetupAbstractionKeys.userName, user.getName())
          .put(SetupAbstractionKeys.userEmail, user.getEmail());
    }
    return orchestrationService.startExecution(planForPipeline, abstractionsBuilder.build());
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return EmbeddedUser.builder().build();
    }
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }
}

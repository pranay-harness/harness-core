package io.harness.cdng.pipeline.helpers;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.mappers.NGPipelineExecutionDTOMapper;
import io.harness.cdng.pipeline.plancreators.PipelinePlanCreator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationService;
import io.harness.exception.InvalidRequestException;
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
import io.harness.yaml.utils.JsonPipelineUtils;

import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
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
      String inputSetPipelineYaml, String eventPayload, boolean useFQNIfErrorResponse, EmbeddedUser user) {
    MergeInputSetResponse mergeInputSetResponse;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      NgPipeline pipeline = inputSetMergeHelper.getOriginalOrTemplatePipeline(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      mergeInputSetResponse = MergeInputSetResponse.builder().mergedPipeline(pipeline).build();
    } else {
      mergeInputSetResponse = inputSetMergeHelper.getMergePipelineYamlFromInputSetPipelineYaml(accountId, orgIdentifier,
          projectIdentifier, pipelineIdentifier, inputSetPipelineYaml, false, useFQNIfErrorResponse);
    }
    Map<String, Object> contextAttributes = new HashMap<>();
    if (inputSetPipelineYaml != null) {
      contextAttributes.put(PipelinePlanCreator.INPUT_SET_YAML_KEY, inputSetPipelineYaml);
    }
    if (eventPayload != null) {
      contextAttributes.put(PipelinePlanCreator.EVENT_PAYLOAD_KEY, eventPayload);
    }
    return getPipelineResponseDTO(
        accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse, user, contextAttributes);
  }

  public NGPipelineExecutionResponseDTO runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetReferences,
      boolean useFQNIfErrorResponse, EmbeddedUser user) {
    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, inputSetReferences, true, useFQNIfErrorResponse);
    String inputSetPipeline = "";
    try {
      inputSetPipeline =
          JsonPipelineUtils.writeYamlString(mergeInputSetResponse.getMergedPipeline()).replaceAll("---\n", "");

    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to template");
    }
    mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputSetPipelineYaml(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, "inputSet", mergeInputSetResponse.getMergedPipeline(), false, useFQNIfErrorResponse);

    Map<String, Object> contextAttributes = new HashMap<>();
    contextAttributes.put(PipelinePlanCreator.INPUT_SET_YAML_KEY, inputSetPipeline);
    return getPipelineResponseDTO(
        accountId, orgIdentifier, projectIdentifier, mergeInputSetResponse, user, contextAttributes);
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
      String projectIdentifier, MergeInputSetResponse mergeInputSetResponse, EmbeddedUser user,
      Map<String, Object> contextAttributes) {
    if (mergeInputSetResponse.isErrorResponse()) {
      return NGPipelineExecutionDTOMapper.toNGPipelineResponseDTO(null, mergeInputSetResponse);
    }
    PlanExecution planExecution = startPipelinePlanExecution(accountId, orgIdentifier, projectIdentifier,
        mergeInputSetResponse.getMergedPipeline(), user, contextAttributes);
    return NGPipelineExecutionDTOMapper.toNGPipelineResponseDTO(planExecution, mergeInputSetResponse);
  }

  private PlanExecution startPipelinePlanExecution(String accountId, String orgIdentifier, String projectIdentifier,
      NgPipeline finalPipeline, EmbeddedUser user, Map<String, Object> contextAttributes) {
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

    if (contextAttributes.get(PipelinePlanCreator.EVENT_PAYLOAD_KEY) != null) {
      abstractionsBuilder.put(
          PipelinePlanCreator.EVENT_PAYLOAD_KEY, (String) contextAttributes.get(PipelinePlanCreator.EVENT_PAYLOAD_KEY));
    }

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

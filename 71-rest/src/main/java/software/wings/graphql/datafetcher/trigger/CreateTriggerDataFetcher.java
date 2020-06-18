package software.wings.graphql.datafetcher.trigger;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import software.wings.app.MainConfiguration;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.execution.PipelineExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.schema.type.trigger.QLCreateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.graphql.schema.type.trigger.QLTriggerPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.Map;

public class CreateTriggerDataFetcher extends BaseMutatorDataFetcher<QLCreateTriggerInput, QLTriggerPayload> {
  @Inject AuthHandler authHandler;
  @Inject AppService appService;
  @Inject PipelineService pipelineService;
  @Inject WorkflowService workflowService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject PipelineExecutionController pipelineExecutionController;
  @Inject WorkflowExecutionController workflowExecutionController;
  @Inject MainConfiguration mainConfiguration;
  @Inject ServiceResourceService serviceResourceService;
  private TriggerService triggerService;

  @Inject
  public CreateTriggerDataFetcher(TriggerService triggerService) {
    super(QLCreateTriggerInput.class, QLTriggerPayload.class);
    this.triggerService = triggerService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLTriggerPayload mutateAndFetch(QLCreateTriggerInput parameter, MutationContext mutationContext) {
    try (AutoLogContext ignore0 =
             new AccountLogContext(mutationContext.getAccountId(), AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      final Trigger savedTrigger = triggerService.save(
          prepareTrigger(parameter, mutationContext.getAccountId(), serviceResourceService, artifactStreamService));
      return prepareQLTrigger(savedTrigger, parameter.getClientMutationId(), mutationContext.getAccountId());
    }
  }

  private QLTriggerPayload prepareQLTrigger(Trigger trigger, String clientMutationId, String accountId) {
    QLTriggerBuilder builder = QLTrigger.builder();
    TriggerController.populateTrigger(trigger, builder, mainConfiguration, accountId);
    return QLTriggerPayload.builder().clientMutationId(clientMutationId).trigger(builder.build()).build();
  }

  private Trigger prepareTrigger(QLCreateTriggerInput qlCreateTriggerInput, String accountId,
      ServiceResourceService serviceResourceService, ArtifactStreamService artifactStreamService) {
    List<ArtifactSelection> artifactSelections = null;
    Map<String, String> workflowVariables = null;
    validateTrigger(qlCreateTriggerInput, accountId);

    TriggerCondition triggerCondition = TriggerConditionController.resolveTriggerCondition(
        qlCreateTriggerInput, pipelineService, artifactStreamService);
    if (qlCreateTriggerInput.getAction().getArtifactSelections() != null) {
      artifactSelections = TriggerActionController.resolveArtifactSelections(
          qlCreateTriggerInput, pipelineService, workflowService, serviceResourceService, artifactStreamService);
    }
    if (qlCreateTriggerInput.getAction().getVariables() != null) {
      workflowVariables = TriggerActionController.resolveWorkflowVariables(qlCreateTriggerInput, pipelineService,
          pipelineExecutionController, workflowExecutionController, workflowService, authHandler);
    }

    return Trigger.builder()
        .name(qlCreateTriggerInput.getName().trim())
        .appId(qlCreateTriggerInput.getApplicationId())
        .description(qlCreateTriggerInput.getDescription())
        .condition(triggerCondition)
        .workflowType(TriggerActionController.resolveWorkflowType(qlCreateTriggerInput))
        .artifactSelections(artifactSelections)
        .workflowVariables(workflowVariables)
        .workflowId(qlCreateTriggerInput.getAction().getEntityId())
        .build();
  }

  public void validateTrigger(QLCreateTriggerInput qlTriggerInput, String accountId) {
    if (EmptyPredicate.isEmpty(qlTriggerInput.getApplicationId())) {
      throw new InvalidRequestException("ApplicationId must not be empty", USER);
    }
    if (!accountId.equals(appService.getAccountIdByAppId(qlTriggerInput.getApplicationId()))) {
      throw new InvalidRequestException("ApplicationId doesn't belong to this account", USER);
    }

    if (EmptyPredicate.isEmpty(qlTriggerInput.getName())) {
      throw new InvalidRequestException("Trigger name must not be empty", USER);
    }

    if (EmptyPredicate.isEmpty(qlTriggerInput.getAction().getEntityId())) {
      throw new InvalidRequestException("Entity Id must not be empty", USER);
    }
  }
}

package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerBuilder;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.graphql.schema.type.trigger.QLTriggerPayload;
import software.wings.service.intfc.AppService;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class TriggerController {
  @Inject AppService appService;
  @Inject TriggerActionController triggerActionController;
  @Inject TriggerConditionController triggerConditionController;
  @Inject HPersistence persistence;

  public void populateTrigger(Trigger trigger, QLTriggerBuilder qlTriggerBuilder, String accountId) {
    qlTriggerBuilder.id(trigger.getUuid())
        .name(trigger.getName())
        .description(trigger.getDescription())
        .condition(triggerConditionController.populateTriggerCondition(trigger, accountId))
        .action(triggerActionController.populateTriggerAction(trigger))
        .createdAt(trigger.getCreatedAt())
        .excludeHostsWithSameArtifact(trigger.isExcludeHostsWithSameArtifact())
        .createdBy(UserController.populateUser(trigger.getCreatedBy()));
  }

  public QLTriggerPayload prepareQLTrigger(Trigger trigger, String clientMutationId, String accountId) {
    QLTriggerBuilder builder = QLTrigger.builder();
    populateTrigger(trigger, builder, accountId);
    return QLTriggerPayload.builder().clientMutationId(clientMutationId).trigger(builder.build()).build();
  }

  public Trigger prepareTrigger(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String accountId) {
    validateTrigger(qlCreateOrUpdateTriggerInput, accountId);

    TriggerBuilder triggerBuilder = Trigger.builder();
    triggerBuilder.uuid(qlCreateOrUpdateTriggerInput.getTriggerId());
    triggerBuilder.name(qlCreateOrUpdateTriggerInput.getName().trim());
    triggerBuilder.appId(qlCreateOrUpdateTriggerInput.getApplicationId());
    triggerBuilder.description(qlCreateOrUpdateTriggerInput.getDescription());

    triggerBuilder.workflowType(triggerActionController.resolveWorkflowType(qlCreateOrUpdateTriggerInput));
    triggerBuilder.workflowId(qlCreateOrUpdateTriggerInput.getAction().getEntityId());
    triggerBuilder.artifactSelections(triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput));
    triggerBuilder.workflowVariables(triggerActionController.resolveWorkflowVariables(qlCreateOrUpdateTriggerInput));

    triggerBuilder.condition(triggerConditionController.resolveTriggerCondition(qlCreateOrUpdateTriggerInput));

    return triggerBuilder.build();
  }

  private void validateTrigger(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String accountId) {
    validateUpdateTrigger(qlCreateOrUpdateTriggerInput, accountId);

    if (EmptyPredicate.isEmpty(qlCreateOrUpdateTriggerInput.getApplicationId())) {
      throw new InvalidRequestException("ApplicationId must not be empty", USER);
    }
    if (!accountId.equals(appService.getAccountIdByAppId(qlCreateOrUpdateTriggerInput.getApplicationId()))) {
      throw new InvalidRequestException("ApplicationId doesn't belong to this account", USER);
    }

    if (EmptyPredicate.isEmpty(qlCreateOrUpdateTriggerInput.getName().trim())) {
      throw new InvalidRequestException("Trigger name must not be empty", USER);
    }

    if (EmptyPredicate.isEmpty(qlCreateOrUpdateTriggerInput.getAction().getEntityId())) {
      throw new InvalidRequestException("Entity Id must not be empty", USER);
    }
  }

  private void validateUpdateTrigger(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String accountId) {
    if (EmptyPredicate.isNotEmpty(qlCreateOrUpdateTriggerInput.getTriggerId())) {
      Trigger trigger = persistence.get(Trigger.class, qlCreateOrUpdateTriggerInput.getTriggerId());
      if (trigger == null) {
        throw new InvalidRequestException("Trigger doesn't exist", USER);
      }

      if (!accountId.equals(appService.getAccountIdByAppId(trigger.getAppId()))) {
        throw new InvalidRequestException("Trigger doesn't exist", USER);
      }

      QLExecutionType qlExecutionType =
          trigger.getWorkflowType() == WorkflowType.ORCHESTRATION ? QLExecutionType.WORKFLOW : QLExecutionType.PIPELINE;

      if (qlCreateOrUpdateTriggerInput.getAction().getExecutionType() != qlExecutionType) {
        throw new InvalidRequestException("Execution Type cannot be modified", USER);
      }
    }
  }
}

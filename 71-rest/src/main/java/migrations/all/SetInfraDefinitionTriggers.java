package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import io.harness.beans.WorkflowType;
import io.harness.exception.ExceptionUtils;
import io.harness.expression.ExpressionEvaluator;
import io.harness.mongo.SampleEntity.SampleEntityKeys;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.EntityType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Trigger;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

@Slf4j
public class SetInfraDefinitionTriggers implements Migration {
  @Inject private TriggerService triggerService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  private static final String accountId = "zEaak-FLS425IEO7OLzMUg";

  @Override
  public void migrate() {
    logger.info("Running infra migration for Triggers. Retrieving applications for accountId: " + accountId);
    List<String> apps = appService.getAppIdsByAccountId(accountId);

    if (isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }
    logger.info("Updating {} applications.", apps.size());
    for (String appId : apps) {
      migrate(appId);
    }
    // migrate("d1Z4dCeET12A2epYnEpmvw");
  }

  public void migrate(String appId) {
    SortOrder sortOrder = new SortOrder();
    sortOrder.setFieldName(SampleEntityKeys.createdAt);
    sortOrder.setOrderType(OrderType.DESC);
    List<Trigger> triggers =
        triggerService
            .list(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, appId).addOrder(sortOrder).build(), false,
                null)
            .getResponse();

    logger.info("Updating {} triggers.", triggers.size());
    for (Trigger trigger : triggers) {
      try {
        migrate(trigger);
      } catch (Exception e) {
        logger.error("[INFRA_MIGRATION_ERROR] Migration failed for triggerId: " + trigger.getUuid()
            + ExceptionUtils.getMessage(e));
      }
    }
  }

  public void migrate(Trigger trigger) {
    boolean modified = false;

    if (isEmpty(trigger.getWorkflowVariables())) {
      logger.info("No migration required for as no workflow variables present. TriggerId: " + trigger.getUuid());
      return;
    }

    modified = migrateWorkflowVariables(trigger);

    if (modified) {
      try {
        triggerService.update(trigger);
        logger.info("--- Trigger updated: {}, {}", trigger.getUuid(), trigger.getName());
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("[INFRA_MIGRATION_ERROR] Error updating trigger: " + trigger.getUuid(), e);
      }
    }
  }

  private boolean migrateWorkflowVariables(Trigger trigger) {
    String workflowId = trigger.getWorkflowId();

    if (trigger.getWorkflowType().equals(WorkflowType.PIPELINE)) {
      logger.info("[INFRA_MIGRATION_INFO] No migration should be needed for pipelines variables in trigger: "
          + trigger.getUuid());
      return false;
    }

    Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), workflowId);
    notNullCheck("workflow is null, workflowId: " + workflowId, workflow);
    notNullCheck("orchestrationWorkflow is null in workflow: " + workflowId, workflow.getOrchestrationWorkflow());

    if (isEmpty(workflow.getOrchestrationWorkflow().getUserVariables())) {
      logger.info(
          "[INFRA_MIGRATION_INFO] Trigger has workflow variables but workflow does not have userVariables. TriggerId: "
          + trigger.getUuid());
      return false;
    }

    List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();

    Variable infraUserVariable = null;
    for (Variable userVariable : userVariables) {
      if (userVariable.obtainEntityType() != null
          && userVariable.obtainEntityType().equals(EntityType.INFRASTRUCTURE_MAPPING)) {
        infraUserVariable = userVariable;
        break;
      }
    }

    if (infraUserVariable == null) {
      logger.info(
          "[INFRA_MIGRATION_INFO] Trigger with workflow where infraMapping not templatised. skipping migration. TriggerId: "
          + trigger.getUuid());
      return false;
    }

    String variableName = infraUserVariable.getName();

    if (!trigger.getWorkflowVariables().containsKey(variableName)) {
      // Workflow has infraMapping templatised but trigger doesn't have infraMapping variable. Invalid trigger
      logger.info(
          "[INFRA_MIGRATION_INFO]Workflow has infra Mapping templatised but trigger does not have infra mapping variable. TriggerId: "
          + trigger.getUuid());
      return false;
    }

    // infra definition variable name
    String infraDefVariableName =
        WorkflowServiceTemplateHelper.getInfraDefVariableNameFromInfraMappingVariableName(variableName);

    String infraMappingId = trigger.getWorkflowVariables().get(variableName);

    // Couldn't find a method which directly tells if trigger is parametrized or not similar to pipeline.

    if (ExpressionEvaluator.containsVariablePattern(infraMappingId)) {
      trigger.getWorkflowVariables().put(infraDefVariableName, infraMappingId);
      trigger.getWorkflowVariables().remove(variableName);
      return true;
    } else {
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(trigger.getAppId(), infraMappingId);
      if (infrastructureMapping == null) {
        logger.info("[INFRA_MIGRATION_INFO]Couldn't fetch infraMapping for trigger. Trigger:  " + trigger.getUuid()
            + "infraMappingId: " + infraMappingId);

        // Removing infraMapping variable as it does not have a valid infraMappingId. removing will mark workflow
        // incomplete and later user can complete the workflow
        trigger.getWorkflowVariables().remove(variableName);
        return true;
      }

      String infraDefId = infrastructureMapping.getInfrastructureDefinitionId();
      if (isEmpty(infraDefId)) {
        // infra definition migration might have failed.Needs manual intervention
        logger.error("[INFRA_MIGRATION_ERROR]Couldn't find infraDefinition id  for trigger. Trigger:  "
            + trigger.getUuid() + "infraMappingId: " + infraMappingId);

        return false;
      }
      trigger.getWorkflowVariables().put(infraDefVariableName, infraDefId);
      trigger.getWorkflowVariables().remove(variableName);
      return true;
    }
  }
}

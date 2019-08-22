package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Account;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class SetInfraDefinitionWorkflows {
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  private static final String accountId = "zEaak-FLS425IEO7OLzMUg";
  private final String DEBUG_LINE = " INFRA_MAPPING_MIGRATION: ";
  @Inject private WingsPersistence wingsPersistence;

  public void migrate(Account account) {
    logger.info(StringUtils.join(
        DEBUG_LINE, "Starting Infra Definition migration for Workflows, accountId ", account.getUuid()));

    long workflowsSize =
        wingsPersistence.createQuery(Workflow.class).filter(WorkflowKeys.accountId, account.getUuid()).count();
    logger.info("Total workflows for account = " + workflowsSize);

    int numberOfPages = (int) ((workflowsSize + 999) / 1000);
    List<Workflow> workflows = new ArrayList<>();
    for (int i = 0; i < numberOfPages; i++) {
      List<Workflow> newWorkflows = workflowService
                                        .listWorkflows(aPageRequest()
                                                           .withLimit(UNLIMITED)
                                                           .withOffset(String.valueOf(i * 1000))
                                                           .addFilter(WorkflowKeys.accountId, EQ, account.getUuid())
                                                           .build())
                                        .getResponse();
      if (!isEmpty(newWorkflows)) {
        workflows.addAll(newWorkflows);
      }
    }

    logger.info("Updating {} workflows.", workflows.size());
    for (Workflow workflow : workflows) {
      try {
        logger.info(StringUtils.join(
            DEBUG_LINE, "Starting Infra Definition migration for Workflow, workflowId ", workflow.getUuid()));
        migrate(workflow);
      } catch (Exception e) {
        logger.error("[INFRA_MIGRATION_ERROR] Migration failed for WorkflowId: " + workflow.getUuid()
            + ExceptionUtils.getMessage(e));
      }
    }
  }

  public void migrate(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    notNullCheck(
        "orchestrationWorkflow is null in workflow: " + workflow.getUuid(), workflow.getOrchestrationWorkflow());

    boolean modified;

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    OrchestrationWorkflowType orchestrationWorkflowType = canaryOrchestrationWorkflow.getOrchestrationWorkflowType();

    // No migration for Build workflows.
    if (orchestrationWorkflowType.equals(OrchestrationWorkflowType.BUILD)) {
      return;
    }

    // Set infra definition Id in workflowPhases and rollback workflow phases irrespective of templatisation

    modified = migrateWorkflowPhases(workflow, canaryOrchestrationWorkflow.getWorkflowPhases(), workflow.getAppId());
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();

    boolean rollbackModified = false;
    if (!isEmpty(rollbackWorkflowPhaseIdMap)) {
      rollbackModified = migrateRollbackWorkflowPhaseIdMap(workflow, rollbackWorkflowPhaseIdMap, workflow.getAppId());
    }
    modified = modified || rollbackModified;

    switch (orchestrationWorkflowType) {
      case BASIC:
      case BLUE_GREEN:
      case ROLLING:
        // check infra templatised in workflow
        if (!isEmpty(workflow.getTemplateExpressions())
            && workflow.getTemplateExpressions()
                    .stream()
                    .filter(t -> t.getFieldName().equals(WorkflowKeys.infraMappingId))
                    .findFirst()
                    .orElse(null)
                != null) {
          modified = true;
          migrateTemplateExpressions(workflow);
        }
        break;
      case CANARY:
      case MULTI_SERVICE:

        for (WorkflowPhase workflowPhase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
          if (workflowPhase.checkInfraTemplatized()) {
            migrateTemplateExpressions(workflowPhase);
            modified = true;
          }
        }
        break;
      default:
        logger.error("[INFRA_MIGRATION_ERROR] are you kidding me, workflowId " + workflow.getUuid());
    }

    if (modified) {
      try {
        workflowService.updateWorkflow(workflow, true);
        logger.info("--- Workflow updated: {}, {}", workflow.getUuid(), workflow.getName());
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("[INFRA_MIGRATION_ERROR] Error updating workflow " + workflow.getUuid(), e);
      }
    }
  }

  private void migrateTemplateExpressions(WorkflowPhase workflowPhase) {
    List<TemplateExpression> templateExpressions = workflowPhase.getTemplateExpressions();
    TemplateExpression infraMappingTemplateExpression =
        workflowPhase.getTemplateExpressions()
            .stream()
            .filter(t -> t.getFieldName().equals(WorkflowKeys.infraMappingId))
            .findFirst()
            .get();
    String infraMappingExpression = infraMappingTemplateExpression.getExpression();
    String infraDefExpression =
        WorkflowServiceTemplateHelper.getInfraDefExpressionFromInfraMappingExpression(infraMappingExpression);
    infraMappingTemplateExpression.setExpression(infraDefExpression);
    infraMappingTemplateExpression.setFieldName(WorkflowKeys.infraDefinitionId);
    infraMappingTemplateExpression.getMetadata().put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION.name());

    if (workflowPhase.checkServiceTemplatized()) {
      logger.info("Service is also tempaltised. Updating relatedField");
      TemplateExpression serviceExpression = workflowPhase.getTemplateExpressions()
                                                 .stream()
                                                 .filter(t -> t.getFieldName().equals(WorkflowKeys.serviceId))
                                                 .findFirst()
                                                 .get();
      serviceExpression.getMetadata().put(Variable.RELATED_FIELD, infraDefExpression);
    }
  }

  private void migrateTemplateExpressions(Workflow workflow) {
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    TemplateExpression infraMappingTemplateExpression =
        workflow.getTemplateExpressions()
            .stream()
            .filter(t -> t.getFieldName().equals(WorkflowKeys.infraMappingId))
            .findFirst()
            .get();
    String infraMappingExpression = infraMappingTemplateExpression.getExpression();
    String infraDefExpression =
        WorkflowServiceTemplateHelper.getInfraDefExpressionFromInfraMappingExpression(infraMappingExpression);
    infraMappingTemplateExpression.setExpression(infraDefExpression);
    infraMappingTemplateExpression.setFieldName(WorkflowKeys.infraDefinitionId);
    infraMappingTemplateExpression.getMetadata().put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION.name());

    TemplateExpression serviceExpression = workflow.getTemplateExpressions()
                                               .stream()
                                               .filter(t -> t.getFieldName().equals(WorkflowKeys.serviceId))
                                               .findFirst()
                                               .orElse(null);
    if (serviceExpression != null) {
      logger.info("Service is also tempaltised. Updating relatedField");
      serviceExpression.getMetadata().put(Variable.RELATED_FIELD, infraDefExpression);
    }
  }

  private boolean migrateWorkflowPhases(Workflow workflow, List<WorkflowPhase> workflowPhases, String appId) {
    List<Boolean> modified = new ArrayList<>();
    logger.info(StringUtils.join(DEBUG_LINE, "Migrating workflowPhases, ", workflowPhases.size()));
    for (WorkflowPhase workflowPhase : workflowPhases) {
      logger.info(StringUtils.join(DEBUG_LINE, "Starting Migration for  workflowPhase ", workflowPhase.getUuid()));
      if (workflowPhase.getInfraDefinitionId() != null) {
        logger.info(
            "[INFRA_MIGRATION_INFO]WorkflowPhase already has infraDefinitionId, no migration needed, WorkflowId: "
            + workflow.getUuid());
        continue;
      }
      modified.add(setInfraDefFromInfraMapping(workflow, workflowPhase));
    }
    if (isEmpty(modified)) {
      return false;
    }
    return modified.stream().anyMatch(t -> t.equals(true));
  }

  private boolean migrateRollbackWorkflowPhaseIdMap(
      Workflow workflow, Map<String, WorkflowPhase> workflowPhases, String appId) {
    List<Boolean> modified = new ArrayList<>();
    logger.info(StringUtils.join(DEBUG_LINE, "Migrating Rollback workflowPhases, ", workflowPhases.size()));
    for (WorkflowPhase workflowPhase : workflowPhases.values()) {
      logger.info(
          StringUtils.join(DEBUG_LINE, "Starting Migration for  rollback workflowPhase ", workflowPhase.getUuid()));
      if (workflowPhase.getInfraDefinitionId() != null) {
        logger.info(
            "[INFRA_MIGRATION_INFO]WorkflowPhase already has infraDefinitionId, no migration needed. WorkfLowId: "
            + workflow.getUuid());
        continue;
      }
      modified.add(setInfraDefFromInfraMapping(workflow, workflowPhase));
    }
    if (isEmpty(modified)) {
      return false;
    }
    return modified.stream().anyMatch(t -> t.equals(true));
  }

  private boolean setInfraDefFromInfraMapping(Workflow workflow, WorkflowPhase workflowPhase) {
    String infraMappingId = workflowPhase.getInfraMappingId();
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(workflow.getAppId(), infraMappingId);
    if (infrastructureMapping == null) {
      logger.error("[INFRA_MIGRATION_INFO] Infra Mapping does not exist " + infraMappingId
          + " skipping migration, WorkflowId: " + workflow.getUuid());

      // This workflow is not usable.Should i remove infraMapping ID here from workflow phase? Which should ideally mark
      // workflow as complete.
      return false;
    }

    String infraDefId = infrastructureMapping.getInfrastructureDefinitionId();
    InfrastructureDefinition infraDef = infrastructureDefinitionService.get(workflow.getAppId(), infraDefId);

    if (infraDef == null) {
      // Manual intervention needed
      logger.error("[INFRA_MIGRATION_ERROR]Infra Definition does not exist " + infraDefId
          + "skipping migration, WorkflowId: " + workflow.getUuid());
      return false;
    }

    String infraDefName = infraDef.getName();

    workflowPhase.setInfraDefinitionId(infraDefId);
    workflowPhase.setInfraDefinitionName(infraDefName);
    return true;
  }
}

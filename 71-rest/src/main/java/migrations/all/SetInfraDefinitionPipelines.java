package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.validation.Validator.notNullCheck;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.EntityType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class SetInfraDefinitionPipelines {
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private WingsPersistence wingsPersistence;
  private static final String accountId = "zEaak-FLS425IEO7OLzMUg";
  private final String DEBUG_LINE = " INFRA_MAPPING_MIGRATION: ";

  public void migrate(Account account) {
    logger.info(StringUtils.join(
        DEBUG_LINE, "Starting Infra Definition migration for Pipelines, accountId ", account.getUuid()));

    long pipelineSize =
        wingsPersistence.createQuery(Pipeline.class).filter(PipelineKeys.accountId, account.getUuid()).count();
    logger.info("Total pipelines for account = " + pipelineSize);

    int numberOfPages = (int) ((pipelineSize + 999) / 1000);
    List<Pipeline> pipelines = new ArrayList<>();
    for (int i = 0; i < numberOfPages; i++) {
      List<Pipeline> newPipelines = pipelineService
                                        .listPipelines(aPageRequest()
                                                           .withLimit(UNLIMITED)
                                                           .withOffset(String.valueOf(i * 1000))
                                                           .addFilter(PipelineKeys.accountId, EQ, account.getUuid())
                                                           .build(),
                                            true, 0, false, null)
                                        .getResponse();
      if (!isEmpty(newPipelines)) {
        pipelines.addAll(newPipelines);
      }
    }

    logger.info("Updating {} pipelines.", pipelines.size());
    for (Pipeline pipeline : pipelines) {
      try {
        migrate(pipeline);
      } catch (Exception e) {
        logger.error("[INFRA_MIGRATION_ERROR] Migration failed for PipelineId: " + pipeline.getUuid()
            + ExceptionUtils.getMessage(e));
      }
    }
  }

  public void migrate(Pipeline pipeline) {
    boolean modified = false;
    // Migrate each stage

    Map<String, Workflow> workflowCache = new HashMap<>();
    for (PipelineStage stage : pipeline.getPipelineStages()) {
      PipelineStageElement stageElement = stage.getPipelineStageElements().get(0);

      // No migration needed for approval stage. Hence continue
      if (stageElement.getType().equals(StateType.APPROVAL.name())) {
        logger.info("Approval state needs no migration");
        continue;
      }

      if (isEmpty(stageElement.getWorkflowVariables())) {
        logger.info("No workflow variables, so no migration needed");
        continue;
      }

      boolean modifiedCurrentPhase;
      try {
        modifiedCurrentPhase = migrateWorkflowVariables(pipeline, stageElement, workflowCache);
      } catch (Exception e) {
        logger.error(
            "[INFRA_MIGRATION_ERROR] Skipping migration.Exception in migrating workflowVariables for Pipeline: "
                + pipeline.getUuid(),
            e);
        modifiedCurrentPhase = false;
      }
      modified = modified || modifiedCurrentPhase;
    }

    if (modified) {
      try {
        updatePipelineInMigration(pipeline);
        logger.info("--- Pipeline updated: {}, {}", pipeline.getUuid(), pipeline.getName());
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("[INFRA_MIGRATION_ERROR] Error updating pipeline " + pipeline.getUuid(), e);
      }
    }
  }

  private void updatePipelineInMigration(Pipeline pipeline) {
    Pipeline savedPipeline = wingsPersistence.getWithAppId(Pipeline.class, pipeline.getAppId(), pipeline.getUuid());
    notNullCheck("Pipeline not saved", savedPipeline, USER);

    UpdateOperations<Pipeline> ops = wingsPersistence.createUpdateOperations(Pipeline.class);
    setUnset(ops, "description", pipeline.getDescription());
    setUnset(ops, "name", pipeline.getName());
    setUnset(ops, "pipelineStages", pipeline.getPipelineStages());
    setUnset(ops, "failureStrategies", pipeline.getFailureStrategies());

    wingsPersistence.update(wingsPersistence.createQuery(Pipeline.class)
                                .filter("appId", pipeline.getAppId())
                                .filter(ID_KEY, pipeline.getUuid()),
        ops);
  }

  private boolean migrateWorkflowVariables(
      Pipeline pipeline, PipelineStageElement stageElement, Map<String, Workflow> workflowCache) {
    String workflowId = String.valueOf(stageElement.getProperties().get("workflowId"));
    Workflow workflow;
    if (workflowCache.containsKey(workflowId)) {
      workflow = workflowCache.get(workflowId);
    } else {
      workflow = workflowService.readWorkflow(pipeline.getAppId(), workflowId);
      notNullCheck("workflow is null, workflowId: " + workflowId, workflow);
      notNullCheck("orchestrationWorkflow is null in workflow: " + workflowId, workflow.getOrchestrationWorkflow());
      workflowCache.put(workflowId, workflow);
    }

    if (isEmpty(workflow.getOrchestrationWorkflow().getUserVariables())) {
      logger.info(
          "[INFRA_MIGRATION_INFO] Skipping migration. Pipeline stage has workflow variables but workflow does not have userVariables.PipelineId: "
          + pipeline.getUuid() + " pipelineStageId: " + stageElement.getUuid());
      return false;
    }

    List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    List<Variable> infraUserVariables = new ArrayList<>();
    for (Variable userVariable : userVariables) {
      if (userVariable.obtainEntityType() != null
          && userVariable.obtainEntityType() == EntityType.INFRASTRUCTURE_MAPPING) {
        infraUserVariables.add(userVariable);
      }
    }

    if (isEmpty(infraUserVariables)) {
      logger.info(
          "[INFRA_MIGRATION_INFO] Pipeline stage with workflow where infraMapping not templatised. skipping migration. PipelineId: "
          + pipeline.getUuid() + " pipelineStageId: " + stageElement.getUuid());
      return false;
    }

    for (Variable infraUserVariable : infraUserVariables) {
      String infraMappingVariableName = infraUserVariable.getName();

      if (!stageElement.getWorkflowVariables().containsKey(infraMappingVariableName)) {
        // Workflow has infraMapping templatised but pipeline doesn't have infraMapping variable. Invalid pipeline
        logger.info(
            "[INFRA_MIGRATION_INFO] Workflow has infra Mapping templatised but pipeline stage does not have infra mapping variable. PipelineId: "
            + pipeline.getUuid() + " pipelineStageId: " + stageElement.getUuid());
        continue;
      }
      // new variable name
      String infraDefVariableName =
          WorkflowServiceTemplateHelper.getInfraDefVariableNameFromInfraMappingVariableName(infraMappingVariableName);
      boolean varNameIsSame = infraDefVariableName.equals(infraMappingVariableName);
      String infraMappingId = stageElement.getWorkflowVariables().get(infraMappingVariableName);
      if (matchesVariablePattern(infraMappingId)) {
        stageElement.getWorkflowVariables().put(infraDefVariableName, infraMappingId);
        if (!varNameIsSame) {
          stageElement.getWorkflowVariables().remove(infraMappingVariableName);
        }

      } else {
        InfrastructureMapping infrastructureMapping =
            infrastructureMappingService.get(pipeline.getAppId(), infraMappingId);
        if (infrastructureMapping == null) {
          logger.info(
              "[INFRA_MIGRATION_INFO] Couldn't fetch infraMapping for pipeline. Pipeline:  " + pipeline.getUuid()
              + " infraMappingId: " + infraMappingId + " pipelineStageId: " + stageElement.getUuid());
          // Removing infraMapping variable as it does not have a valid infraMappingId. removing will mark workflow
          // incomplete and later user can complete the workflow
          stageElement.getWorkflowVariables().remove(infraMappingVariableName);
          continue;
        }

        String infraDefId = infrastructureMapping.getInfrastructureDefinitionId();
        if (isEmpty(infraDefId)) {
          logger.error(
              "[INFRA_MIGRATION_ERROR]Couldn't find infraDefinition id  for pipeline. Pipeline:  " + pipeline.getUuid()
              + "infraMappingId: " + infraMappingId + " pipelineStageId: " + stageElement.getUuid());
          continue;
        }
        stageElement.getWorkflowVariables().put(infraDefVariableName, infraDefId);
        if (!varNameIsSame) {
          stageElement.getWorkflowVariables().remove(infraMappingVariableName);
        }
      }
    }
    return true;
  }
}

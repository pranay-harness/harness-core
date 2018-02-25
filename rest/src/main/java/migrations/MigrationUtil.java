package migrations;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.mongodb.BasicDBList;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MigrationUtil {
  private static final Logger logger = LoggerFactory.getLogger(MigrationUtil.class);

  /*
   * Be sure to make corresponding changes in UI that reference the old state type.
   *
   * Check:
   *   StencilConfig.js
   *   StencilModal.js
   *
   * StateTypes with StencilType CLOUD will continue to show in the list of commands available in workflows
   */
  public static void renameStateTypeAndStateClass(StateType oldStateType, StateType newStateType,
      WingsPersistence wingsPersistence, WorkflowService workflowService) {
    logger.info("Renaming {} to {} in all CanaryOrchestrationWorkflows", oldStateType.name(), newStateType.name());
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    logger.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }
    logger.info("Checking {} applications", apps.size());
    for (Application app : apps) {
      List<Workflow> workflows =
          workflowService
              .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, app.getUuid()).build())
              .getResponse();
      int updateCount = 0;
      for (Workflow workflow : workflows) {
        boolean workflowModified = false;
        if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
          CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
            List<WorkflowPhase> both = new ArrayList<>();
            both.add(workflowPhase);
            WorkflowPhase rollbackPhase = coWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
            if (rollbackPhase != null) {
              both.add(rollbackPhase);
            }
            for (WorkflowPhase phase : both) {
              for (PhaseStep phaseStep : phase.getPhaseSteps()) {
                for (GraphNode node : phaseStep.getSteps()) {
                  if (oldStateType.name().equals(node.getType())) {
                    workflowModified = true;
                    node.setType(newStateType.name());
                    Map<String, Object> properties = node.getProperties();
                    properties.put("className", newStateType.getTypeClass().getCanonicalName());
                  }
                }
              }
            }
          }
        }
        if (workflowModified) {
          try {
            logger.info("... Updating workflow: {} - {}", workflow.getUuid(), workflow.getName());
            workflowService.updateWorkflow(workflow);
            Thread.sleep(100);
          } catch (Exception e) {
            logger.error("Error updating workflow", e);
          }
          updateCount++;
        }
      }
      logger.info("Application migrated: {} - {}. Updated {} out of {} workflows", app.getUuid(), app.getName(),
          updateCount, workflows.size());
    }
  }

  public static void removeDelegateTaskType(String taskToRemove, WingsPersistence wingsPersistence) {
    logger.info("Removing " + taskToRemove + " from supported delegate tasks");
    DBCursor delegates = wingsPersistence.getCollection("delegates").find();
    while (delegates.hasNext()) {
      DBObject next = delegates.next();
      String uuId = (String) next.get("_id");
      logger.info("updating delegate {}", uuId);
      BasicDBList supportedTaskTypes = (BasicDBList) next.get("supportedTaskTypes");
      for (Iterator iterator = supportedTaskTypes.iterator(); iterator.hasNext();) {
        String taskType = (String) iterator.next();
        if (taskType.equals(taskToRemove)) {
          logger.info("Removing {} from supported tasks for delegate {}", taskToRemove, uuId);
          iterator.remove();
        }
      }

      logger.info("setting supported tasks for delegate {} to {} ", uuId, supportedTaskTypes);
      next.put("supportedTaskTypes", supportedTaskTypes);
      wingsPersistence.getCollection("delegates").save(next);
    }
    logger.info("Done removing " + taskToRemove + " from supported delegate tasks");
  }
}

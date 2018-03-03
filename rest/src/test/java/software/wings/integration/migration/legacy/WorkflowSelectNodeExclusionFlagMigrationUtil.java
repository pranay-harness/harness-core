package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.PhaseStepType.PROVISION_NODE;
import static software.wings.beans.PhaseStepType.SELECT_NODE;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.SearchFilter;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

/**
 * Migration script to set node select exclusion true by default
 * @author brett on 10/11/17
 */
@Integration
@Ignore
public class WorkflowSelectNodeExclusionFlagMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Test
  public void setSelectNodeExclusionFlag() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      System.out.println("No applications found");
      return;
    }
    System.out.println("Updating " + apps.size() + " applications.");
    StringBuilder result = new StringBuilder(64);
    for (Application app : apps) {
      List<Workflow> workflows = workflowService
                                     .listWorkflows(aPageRequest()
                                                        .withLimit(UNLIMITED)
                                                        .addFilter("appId", SearchFilter.Operator.EQ, app.getUuid())
                                                        .build())
                                     .getResponse();
      int updateCount = 0;
      int candidateCount = 0;
      for (Workflow workflow : workflows) {
        boolean workflowModified = false;
        boolean candidateFound = false;
        if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
          CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
            for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
              if (SELECT_NODE == phaseStep.getPhaseStepType() || PROVISION_NODE == phaseStep.getPhaseStepType()) {
                candidateFound = true;
                for (GraphNode node : phaseStep.getSteps()) {
                  if (StateType.AWS_NODE_SELECT.name().equals(node.getType())
                      || StateType.DC_NODE_SELECT.name().equals(node.getType())) {
                    Map<String, Object> properties = node.getProperties();
                    if (!properties.containsKey("excludeSelectedHostsFromFuturePhases")) {
                      workflowModified = true;
                      properties.put("excludeSelectedHostsFromFuturePhases", true);
                    }
                  }
                }
              }
            }
          }
        }
        if (candidateFound) {
          candidateCount++;
        }
        if (workflowModified) {
          result.append("\n***** account: [")
              .append(app.getAccountId())
              .append("], app: [")
              .append(app.getName())
              .append("], workflow: {")
              .append(workflow.getName())
              .append("]\n\n");

          // try {
          //   workflowService.updateWorkflow(workflow);
          //   sleep(ofMillis(500));
          // } catch (Exception e) {
          //   e.printStackTrace();
          // }

          updateCount++;
        }
      }
      if (candidateCount > 0) {
        result.append("Application migrated: ")
            .append(app.getName())
            .append(". Updated ")
            .append(updateCount)
            .append(" workflows out of ")
            .append(candidateCount)
            .append(" candidates.\n");
      }
    }
    System.out.println(result.toString());
  }
}

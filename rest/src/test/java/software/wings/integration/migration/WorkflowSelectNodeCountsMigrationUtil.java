package software.wings.integration.migration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.PhaseStepType.PROVISION_NODE;
import static software.wings.beans.PhaseStepType.SELECT_NODE;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Graph;
import software.wings.beans.InstanceUnitType;
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
import java.util.Set;

/**
 * Migration script to make node select counts cumulative
 * @author brett on 10/3/17
 */
@Integration
@Ignore
public class WorkflowSelectNodeCountsMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Test
  public void setSelectNodeCounts() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      System.out.println("No applications found");
      return;
    }
    System.out.println("Updating " + apps.size() + " applications.");
    StringBuilder result = new StringBuilder();
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
          int runningTotal = 0;
          Multimap<String, WorkflowPhase> infraPhaseMap = ArrayListMultimap.create();
          for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
            infraPhaseMap.put(workflowPhase.getInfraMappingId(), workflowPhase);
          }
          Set<String> infraIds = infraPhaseMap.keySet();
          for (String infraId : infraIds) {
            for (WorkflowPhase workflowPhase : infraPhaseMap.get(infraId)) {
              for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
                if (SELECT_NODE == phaseStep.getPhaseStepType() || PROVISION_NODE == phaseStep.getPhaseStepType()) {
                  if (!candidateFound && infraIds.size() > 1) {
                    System.out.println("**** More than 1 infra mapping. Acct:" + app.getAccountId()
                        + " App:" + app.getUuid() + " Workflow:" + workflow.getUuid());
                  }
                  candidateFound = true;
                  for (Graph.Node node : phaseStep.getSteps()) {
                    if (StateType.AWS_NODE_SELECT.name().equals(node.getType())
                        || StateType.DC_NODE_SELECT.name().equals(node.getType())) {
                      Map<String, Object> properties = node.getProperties();
                      if (properties.containsKey("instanceCount") && !properties.containsKey("instanceUnitType")) {
                        if (!workflowModified) {
                          System.out.println(
                              "\n" + (updateCount + 1) + ": " + coWorkflow.getWorkflowPhases().size() + " phases");
                        }
                        workflowModified = true;
                        int instanceCount = (Integer) properties.get("instanceCount");
                        runningTotal += instanceCount;
                        properties.put("instanceCount", runningTotal);
                        properties.put("instanceUnitType", InstanceUnitType.COUNT);
                        properties.remove("provisionNode");
                        properties.remove("launcherConfigName");
                        System.out.println("properties = " + properties);
                      }
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
          try {
            workflowService.updateWorkflow(workflow);
            Thread.sleep(100);
          } catch (Exception e) {
            e.printStackTrace();
          }

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

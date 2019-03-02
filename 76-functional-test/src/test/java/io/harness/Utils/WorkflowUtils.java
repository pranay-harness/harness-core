package io.harness.Utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.sm.StateType.HTTP;

import com.google.common.collect.ImmutableMap;

import io.harness.RestUtils.WorkflowRestUtil;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.sm.states.HttpState;

import java.util.Collections;

public class WorkflowUtils {
  WorkflowRestUtil workflowRestUtil = new WorkflowRestUtil();

  public WorkflowPhase modifyPhases(Workflow savedWorkflow, String applicationId) {
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow();
    for (WorkflowPhase workflowPhase : orchestrationWorkflow.getWorkflowPhases()) {
      if (workflowPhase.getName().equalsIgnoreCase("Phase 1")) {
        for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
          if (phaseStep.getPhaseStepType().equals(PhaseStepType.VERIFY_SERVICE)) {
            phaseStep.setSteps(Collections.singletonList(getHTTPNode("Test")));
            break;
          }
        }
        return workflowRestUtil.saveWorkflowPhase(
            applicationId, savedWorkflow.getUuid(), workflowPhase.getUuid(), workflowPhase);
      }
    }
    return null;
  }

  public GraphNode getHTTPNode(String... values) {
    HttpState httpState = new HttpState();
    httpState.setHeader("${serviceVariables.normalText}");
    return GraphNode.builder()
        .id(generateUuid())
        .type(HTTP.name())
        .name("HTTP")
        .properties(
            ImmutableMap.<String, Object>builder()
                .put(HttpState.URL_KEY, "https://postman-echo.com/post")
                .put(HttpState.METHOD_KEY, "POST")
                .put(HttpState.HEADER,
                    "${serviceVariable.normalText}:" + values[0] + ", ${serviceVariable.overridableText}:" + values[0])
                .build())
        .build();
  }
}

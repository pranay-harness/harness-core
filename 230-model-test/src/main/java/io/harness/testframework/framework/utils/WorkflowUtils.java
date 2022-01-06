/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.sm.StateType.HTTP;

import io.harness.beans.KeyValuePair;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.HttpState.HttpStateKeys;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;

public class WorkflowUtils {
  public static WorkflowPhase modifyPhases(String bearerToken, Workflow savedWorkflow, String applicationId) {
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow();
    for (WorkflowPhase workflowPhase : orchestrationWorkflow.getWorkflowPhases()) {
      if (workflowPhase.getName().equalsIgnoreCase("Phase 1")) {
        for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
          if (phaseStep.getPhaseStepType() == PhaseStepType.VERIFY_SERVICE) {
            phaseStep.setSteps(Collections.singletonList(getHTTPNode("Test")));
            break;
          }
        }
        return WorkflowRestUtils.saveWorkflowPhase(
            bearerToken, applicationId, savedWorkflow.getUuid(), workflowPhase.getUuid(), workflowPhase);
      }
    }
    return null;
  }

  public static WorkflowPhase modifyPhasesForPipeline(
      String bearerToken, Workflow savedWorkflow, String applicationId) {
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow();
    for (WorkflowPhase workflowPhase : orchestrationWorkflow.getWorkflowPhases()) {
      if (workflowPhase.getName().equalsIgnoreCase("Phase 1")) {
        for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
          if (phaseStep.getPhaseStepType() == PhaseStepType.VERIFY_SERVICE) {
            phaseStep.setSteps(Collections.singletonList(getHTTPNode()));
          } else {
            phaseStep.setSteps(new ArrayList<>());
          }
        }
        return WorkflowRestUtils.saveWorkflowPhase(
            bearerToken, applicationId, savedWorkflow.getUuid(), workflowPhase.getUuid(), workflowPhase);
      }
    }
    return null;
  }

  public static GraphNode getHTTPNode(String... values) {
    HttpState httpState = new HttpState();
    httpState.setHeader("${serviceVariables.normalText}");
    return GraphNode.builder()
        .id(generateUuid())
        .type(HTTP.name())
        .name("HTTP")
        .properties(
            ImmutableMap.<String, Object>builder()
                .put(HttpStateKeys.url, "https://postman-echo.com/post")
                .put(HttpStateKeys.method, "POST")
                .put(HttpStateKeys.headers,
                    Lists.newArrayList(
                        KeyValuePair.builder().key("${serviceVariable.normalText}").value(values[0]).build(),
                        KeyValuePair.builder().key("${serviceVariable.overridableText}").value(values[0]).build()))
                .build())
        .build();
  }

  public static GraphNode getHTTPNode() {
    return GraphNode.builder()
        .id(generateUuid())
        .type(HTTP.name())
        .name("HTTP")
        .properties(ImmutableMap.<String, Object>builder()
                        .put(HttpStateKeys.url, "https://www.google.com")
                        .put(HttpStateKeys.method, "GET")
                        .build())
        .build();
  }
}

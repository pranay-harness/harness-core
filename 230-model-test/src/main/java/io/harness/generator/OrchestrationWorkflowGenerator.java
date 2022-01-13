/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;

import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

@Singleton
public class OrchestrationWorkflowGenerator {
  public OrchestrationWorkflow createOrchestrationWorkflow(Randomizer.Seed seed) {
    Graph graph =
        aGraph()
            .addNodes(GraphNode.builder().id("n1").name("stop").type(StateType.ENV_STATE.name()).origin(true).build(),
                GraphNode.builder()
                    .id("n2")
                    .name("wait")
                    .type(StateType.WAIT.name())
                    .properties(ImmutableMap.<String, Object>builder().put("duration", 1l).build())
                    .build(),
                GraphNode.builder().id("n3").name("start").type(StateType.ENV_STATE.name()).build())
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
            .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
            .build();

    return aCustomOrchestrationWorkflow().withGraph(graph).build();
  }
}

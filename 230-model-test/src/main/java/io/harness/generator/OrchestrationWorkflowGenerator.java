/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

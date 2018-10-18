package io.harness.generator;

import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;

import com.google.inject.Singleton;

import software.wings.beans.Graph;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.sm.StateType;

@Singleton
public class OrchestrationWorkflowGenerator {
  public OrchestrationWorkflow createOrchestrationWorkflow(Randomizer.Seed seed) {
    Graph graph = aGraph()
                      .addNodes(aGraphNode()
                                    .withId("n1")
                                    .withName("stop")
                                    .withType(StateType.ENV_STATE.name())
                                    .withOrigin(true)
                                    .build(),
                          aGraphNode()
                              .withId("n2")
                              .withName("wait")
                              .withType(StateType.WAIT.name())
                              .addProperty("duration", 1l)
                              .build(),
                          aGraphNode().withId("n3").withName("start").withType(StateType.ENV_STATE.name()).build())
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
                      .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
                      .build();

    return aCustomOrchestrationWorkflow().withGraph(graph).build();
  }
}

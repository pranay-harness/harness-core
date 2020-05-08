package io.harness.executionplan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graph.Graph;
import graph.StepGraph;
import io.harness.beans.steps.CIStep;
import io.harness.node.BasicStepToExecutionNodeConverter;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;

import java.util.List;

/**
 * Converts Steps Graph to Execution Plan by iterating all steps
 */

// TODO Register in module instead of having singleton
@Singleton
public class BasicExecutionPlanGenerator implements ExecutionPlanGenerator<CIStep> {
  @Inject private BasicStepToExecutionNodeConverter basicStepToExecutionNodeConverter;
  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String APP_ID = "XEsfW6D_RJm1IaGpDidD3g";

  @Override
  public Plan generateExecutionPlan(Graph<CIStep> graph) {
    StepGraph ciStepsGraph = (StepGraph) graph;
    List<ExecutionNode> executionNodeList =
        ciStepsGraph.getCiSteps()
            .stream()
            .map(ciStep -> basicStepToExecutionNodeConverter.convertStep(ciStep, ciStepsGraph.getNextNodeUuid(ciStep)))
            .collect(toList());

    return Plan.builder()
        .nodes(executionNodeList)
        .startingNodeId(ciStepsGraph.getStartNodeUuid())
        .setupAbstractions(
            ImmutableMap.<String, String>builder().put("accountId", ACCOUNT_ID).put("appId", APP_ID).build())
        .uuid(generateUuid())
        .build();
  }
}

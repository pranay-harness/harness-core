package io.harness.executionplan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;

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
        .uuid(generateUuid())
        .build();
  }
}

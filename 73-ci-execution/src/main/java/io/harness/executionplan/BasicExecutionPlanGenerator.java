package io.harness.executionplan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graph.StepInfoGraph;
import graph.StepInfoGraphConverter;
import io.harness.node.BasicStepToExecutionNodeConverter;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.yaml.core.Execution;

import java.util.List;

/**
 * Converts Steps Graph to Execution Plan by iterating all steps
 */

// TODO Register in module instead of having singleton
@Singleton
public class BasicExecutionPlanGenerator implements ExecutionPlanGenerator {
  @Inject private BasicStepToExecutionNodeConverter basicStepToExecutionNodeConverter;
  @Inject private StepInfoGraphConverter graphConverter;

  @Override
  public Plan generateExecutionPlan(Execution execution) {
    StepInfoGraph ciStepsGraph = getStepInfos(execution);
    List<PlanNode> planNodeList =
        ciStepsGraph.getSteps()
            .stream()
            .map(ciStep -> basicStepToExecutionNodeConverter.convertStep(ciStep, ciStepsGraph.getNextNodeUuids(ciStep)))
            .collect(toList());

    return Plan.builder()
        .nodes(planNodeList)
        .startingNodeId(ciStepsGraph.getStartNodeUuid())
        .uuid(generateUuid())
        .build();
  }

  private StepInfoGraph getStepInfos(Execution execution) {
    return graphConverter.convert(execution.getSteps());
  }
}

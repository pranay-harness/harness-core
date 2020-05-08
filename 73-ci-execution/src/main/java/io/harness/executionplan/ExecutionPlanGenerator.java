package io.harness.executionplan;

import graph.Graph;
import io.harness.beans.steps.Step;
import io.harness.plan.Plan;

/**
 *  Maps pipeline graph to execution plan
 */

public interface ExecutionPlanGenerator<T extends Step> { Plan generateExecutionPlan(Graph<T> graph); }

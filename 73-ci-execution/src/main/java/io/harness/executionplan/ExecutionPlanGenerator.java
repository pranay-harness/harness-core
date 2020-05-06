package io.harness.executionplan;

import graph.StepsGraph;
import io.harness.beans.steps.Step;
import io.harness.plan.Plan;

/**
 *  Maps pipeline graph to execution plan
 */

public interface ExecutionPlanGenerator<T extends Step> { Plan generateExecutionPlan(StepsGraph<T> stepsGraph); }

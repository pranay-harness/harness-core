package io.harness.executionplan;

import io.harness.plan.Plan;
import io.harness.yaml.core.Execution;

/**
 *  Maps pipeline graph to execution plan
 */

public interface ExecutionPlanGenerator { Plan generateExecutionPlan(Execution execution); }

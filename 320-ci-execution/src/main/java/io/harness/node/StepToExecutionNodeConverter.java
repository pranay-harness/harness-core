package io.harness.node;

import io.harness.plan.PlanNode;

import java.util.List;

/**
 * Converts a step to execution Node by adding facilitators and advisers based upon step metadata
 */

public interface StepToExecutionNodeConverter<T> { PlanNode convertStep(T step, List<String> nextStepUuids); }

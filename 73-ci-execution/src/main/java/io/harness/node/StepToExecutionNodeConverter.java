package io.harness.node;

import io.harness.plan.ExecutionNode;

import java.util.List;

/**
 * Converts a step to execution Node by adding facilitators and advisers based upon step metadata
 */

public interface StepToExecutionNodeConverter<T> { ExecutionNode convertStep(T step, List<String> nextStepUuids); }

package io.harness.executionplan.core;

/**
 * helper interface for plan creators defining support definition inline
 * @param <T> object type to plan for
 */
public interface SupportDefinedExecutorPlanCreator<T> extends ExecutionPlanCreator<T>, SupportDefiner {}

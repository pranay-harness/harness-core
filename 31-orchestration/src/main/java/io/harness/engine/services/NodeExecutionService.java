package io.harness.engine.services;

import io.harness.execution.NodeExecution;

import java.util.List;

public interface NodeExecutionService { List<NodeExecution> fetchNodeExecutions(String planExecutionId); }

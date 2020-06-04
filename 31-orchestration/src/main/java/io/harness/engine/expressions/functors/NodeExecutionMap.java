package io.harness.engine.expressions.functors;

import static java.util.Arrays.asList;

import io.harness.ambiance.Ambiance;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.services.OutcomeException;
import io.harness.engine.services.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.LateBindingMap;
import io.harness.references.OutcomeRefObject;
import io.harness.references.SweepingOutputRefObject;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.resolver.sweepingoutput.SweepingOutputException;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * NodeExecutionMap resolves expressions for a single node execution.
 *
 * Suppose the current node has identifier `node1` and we see an expression `node1.child1`:
 * 1. We first try to find a child with identifier `child1`
 * 2. Then we try to find a property of node1's step parameters with name `child1`
 * 3. Then we try to find an outcome in node1's scope with name `child1`
 * 4. Then we try to find an sweeping output in node1's scope with name `child1`
 */
@Value
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NodeExecutionMap extends LateBindingMap {
  transient NodeExecutionsCache nodeExecutionsCache;
  transient OutcomeService outcomeService;
  transient ExecutionSweepingOutputService executionSweepingOutputService;
  transient Ambiance ambiance;
  transient NodeExecution nodeExecution;
  transient Set<NodeExecutionEntityType> entityTypes;
  transient Map<String, Object> children;

  @Builder
  NodeExecutionMap(NodeExecutionsCache nodeExecutionsCache, OutcomeService outcomeService,
      ExecutionSweepingOutputService executionSweepingOutputService, Ambiance ambiance, NodeExecution nodeExecution,
      Set<NodeExecutionEntityType> entityTypes, Map<String, Object> children) {
    this.nodeExecutionsCache = nodeExecutionsCache;
    this.outcomeService = outcomeService;
    this.executionSweepingOutputService = executionSweepingOutputService;
    this.ambiance = ambiance;
    this.nodeExecution = nodeExecution;
    this.entityTypes = entityTypes == null ? NodeExecutionEntityType.allEntities() : entityTypes;
    if (children == null) {
      this.children = Collections.emptyMap();
    } else {
      this.children = new LateBindingMap();
      this.children.putAll(children);
    }
  }

  @Override
  public synchronized Object get(Object key) {
    if (!(key instanceof String)) {
      return null;
    }

    return fetchFirst(asList(this ::fetchChild, this ::fetchStepParameters, this ::fetchOutcomeOrOutput), (String) key);
  }

  private Object fetchFirst(List<Function<String, Optional<Object>>> fns, String key) {
    if (EmptyPredicate.isEmpty(fns)) {
      return null;
    }

    for (Function<String, Optional<Object>> fn : fns) {
      Optional<Object> optional = fn.apply(key);
      if (optional.isPresent()) {
        return optional.get();
      }
    }
    return null;
  }

  private Optional<Object> fetchChild(String key) {
    return children.containsKey(key) ? Optional.of(children.get(key)) : Optional.empty();
  }

  private Optional<Object> fetchStepParameters(String key) {
    if (nodeExecution == null || !entityTypes.contains(NodeExecutionEntityType.STEP_PARAMETERS)) {
      return Optional.empty();
    }

    StepParameters stepParameters = nodeExecution.getResolvedStepParameters() == null
        ? nodeExecution.getNode().getStepParameters()
        : nodeExecution.getResolvedStepParameters();
    return ExpressionEvaluatorUtils.fetchField(stepParameters, key);
  }

  private Optional<Object> fetchOutcomeOrOutput(String key) {
    if (nodeExecution == null
        || (!entityTypes.contains(NodeExecutionEntityType.OUTCOME)
               && !entityTypes.contains(NodeExecutionEntityType.SWEEPING_OUTPUT))) {
      return Optional.empty();
    }

    Ambiance newAmbiance = Ambiance.fromNodeExecution(ambiance.getInputArgs(), nodeExecution);
    if (newAmbiance == null) {
      return Optional.empty();
    }

    Optional<Object> value = fetchOutcome(newAmbiance, key);
    if (!value.isPresent()) {
      value = fetchSweepingOutput(newAmbiance, key);
    }
    return value;
  }

  private Optional<Object> fetchOutcome(Ambiance newAmbiance, String key) {
    if (!entityTypes.contains(NodeExecutionEntityType.OUTCOME)) {
      return Optional.empty();
    }

    try {
      return Optional.ofNullable(outcomeService.resolve(newAmbiance, OutcomeRefObject.builder().name(key).build()));
    } catch (OutcomeException ignored) {
      return Optional.empty();
    }
  }

  private Optional<Object> fetchSweepingOutput(Ambiance newAmbiance, String key) {
    if (!entityTypes.contains(NodeExecutionEntityType.SWEEPING_OUTPUT)) {
      return Optional.empty();
    }

    try {
      return Optional.ofNullable(
          executionSweepingOutputService.resolve(newAmbiance, SweepingOutputRefObject.builder().name(key).build()));
    } catch (SweepingOutputException ignored) {
      return Optional.empty();
    }
  }
}

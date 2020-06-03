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
import io.harness.expression.LateBindingValue;
import io.harness.references.OutcomeRefObject;
import io.harness.references.SweepingOutputRefObject;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.resolver.sweepingoutput.SweepingOutputException;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * NodeExecutionValue implements a LateBindingValue which matches expressions starting from startNodeExecution. If we
 * want to resolve fully qualified expressions, startNodeExecution should be null. OOtherwise, it should be the node
 * execution from where we want to start expression evaluation. It supports step parameters and outcomes in expressions.
 */
@Value
@Builder
public class NodeExecutionValue implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  OutcomeService outcomeService;
  ExecutionSweepingOutputService executionSweepingOutputService;
  Ambiance ambiance;
  NodeExecution startNodeExecution;

  @Override
  public Object bind() {
    Map<String, Object> map = new HashMap<>();
    addChildren(map, startNodeExecution == null ? null : startNodeExecution.getUuid());
    return new NodeExecutionMap(
        nodeExecutionsCache, outcomeService, executionSweepingOutputService, ambiance, startNodeExecution, map);
  }

  private void addChildren(Map<String, Object> map, String nodeExecutionId) {
    List<NodeExecution> children = nodeExecutionsCache.fetchChildren(nodeExecutionId);
    for (NodeExecution child : children) {
      if (canAdd(child)) {
        addToMap(map, child);
        continue;
      }

      addChildren(map, child.getUuid());
    }
  }

  private boolean canAdd(NodeExecution nodeExecution) {
    return !nodeExecution.getNode().isSkipExpressionChain()
        && EmptyPredicate.isNotEmpty(nodeExecution.getNode().getIdentifier());
  }

  private void addToMap(Map<String, Object> map, NodeExecution nodeExecution) {
    String key = nodeExecution.getNode().getIdentifier();
    NodeExecutionValue childValue = NodeExecutionValue.builder()
                                        .nodeExecutionsCache(nodeExecutionsCache)
                                        .outcomeService(outcomeService)
                                        .executionSweepingOutputService(executionSweepingOutputService)
                                        .ambiance(ambiance)
                                        .startNodeExecution(nodeExecution)
                                        .build();
    map.compute(key, (k, v) -> {
      if (v == null) {
        return childValue;
      }

      Object boundChild = childValue.bind();
      if (v instanceof List) {
        ((List<Object>) v).add(boundChild);
        return v;
      }

      Object boundV = (v instanceof NodeExecutionValue) ? ((NodeExecutionValue) v).bind() : v;
      return asList(boundV, boundChild);
    });
  }

  /**
   * NodeExecutionMap resolves expressions for a single node execution.
   *
   * Suppose the current node has identifier `node1` and we see an expression `node1.child1`:
   * 1. We first try to find a child with identifier `child1`
   * 2. Then we try to find a property of node1's step parameters with name `child1`
   * 3. Then we try to find an outcome in node1's scope with name `child1`
   * 4. Then we try to find an sweeping output in node1's scope with name `child1`
   */
  @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
  public static class NodeExecutionMap extends LateBindingMap {
    private final transient NodeExecutionsCache nodeExecutionsCache;
    private final transient OutcomeService outcomeService;
    private final transient ExecutionSweepingOutputService executionSweepingOutputService;
    private final transient Ambiance ambiance;
    private final transient NodeExecution nodeExecution;
    private final transient Map<String, Object> children;

    NodeExecutionMap(NodeExecutionsCache nodeExecutionsCache, OutcomeService outcomeService,
        ExecutionSweepingOutputService executionSweepingOutputService, Ambiance ambiance, NodeExecution nodeExecution,
        Map<String, Object> children) {
      this.nodeExecutionsCache = nodeExecutionsCache;
      this.outcomeService = outcomeService;
      this.executionSweepingOutputService = executionSweepingOutputService;
      this.ambiance = ambiance;
      this.nodeExecution = nodeExecution;
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

      return fetchFirst(
          asList(this ::fetchChild, this ::fetchStepParameters, this ::fetchOutcomeOrOutput), (String) key);
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
      if (nodeExecution == null) {
        return Optional.empty();
      }

      StepParameters stepParameters = nodeExecution.getResolvedStepParameters() == null
          ? nodeExecution.getNode().getStepParameters()
          : nodeExecution.getResolvedStepParameters();
      return ExpressionEvaluatorUtils.fetchField(stepParameters, key);
    }

    private Optional<Object> fetchOutcomeOrOutput(String key) {
      if (nodeExecution == null) {
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
      try {
        return Optional.ofNullable(outcomeService.resolve(newAmbiance, OutcomeRefObject.builder().name(key).build()));
      } catch (OutcomeException ignored) {
        return Optional.empty();
      }
    }

    private Optional<Object> fetchSweepingOutput(Ambiance newAmbiance, String key) {
      try {
        return Optional.ofNullable(
            executionSweepingOutputService.resolve(newAmbiance, SweepingOutputRefObject.builder().name(key).build()));
      } catch (SweepingOutputException ignored) {
        return Optional.empty();
      }
    }
  }
}

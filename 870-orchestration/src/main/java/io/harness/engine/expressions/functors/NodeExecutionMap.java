package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.execution.NodeExecution.NodeExecutionKeys;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.outcomes.OutcomeException;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.engine.outputs.SweepingOutputException;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.execution.NodeExecution;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.LateBindingMap;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.RefObjectUtil;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * NodeExecutionMap resolves expressions for a single node execution.
 *
 * Suppose the current node has identifier `node1` and we see an expression `node1.child1`:
 * 1. We first try to find a child with identifier `child1`
 * 2. Then we try to find a property of node1's step parameters with name `child1`
 * 3. Then we try to find an outcome in node1's scope with name `child1`
 * 4. Then we try to find an sweeping output in node1's scope with name `child1`
 */
@OwnedBy(CDC)
@Value
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NodeExecutionMap extends LateBindingMap {
  transient NodeExecutionsCache nodeExecutionsCache;
  transient OutcomeService outcomeService;
  transient PmsSweepingOutputService pmsSweepingOutputService;
  transient Ambiance ambiance;
  transient NodeExecution nodeExecution;
  transient Set<NodeExecutionEntityType> entityTypes;
  transient Map<String, Object> children;

  @Builder
  NodeExecutionMap(NodeExecutionsCache nodeExecutionsCache, OutcomeService outcomeService,
      PmsSweepingOutputService pmsSweepingOutputService, Ambiance ambiance, NodeExecution nodeExecution,
      Set<NodeExecutionEntityType> entityTypes, Map<String, Object> children) {
    this.nodeExecutionsCache = nodeExecutionsCache;
    this.outcomeService = outcomeService;
    this.pmsSweepingOutputService = pmsSweepingOutputService;
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

    return fetchFirst(
        asList(this::fetchChild, this::fetchNodeExecutionField, this::fetchStepParameters, this::fetchOutcomeOrOutput),
        (String) key);
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

  private Optional<Object> fetchNodeExecutionField(String key) {
    if (nodeExecution == null || !entityTypes.contains(NodeExecutionEntityType.NODE_EXECUTION_FIELDS)) {
      return Optional.empty();
    }

    if (NodeExecutionKeys.status.equals(key)) {
      return nodeExecution.getStatus() == null ? Optional.empty() : Optional.of(nodeExecution.getStatus().name());
    } else if (NodeExecutionKeys.startTs.equals(key)) {
      return Optional.ofNullable(nodeExecution.getStartTs());
    } else if (NodeExecutionKeys.endTs.equals(key)) {
      return Optional.ofNullable(nodeExecution.getEndTs());
    } else {
      return Optional.empty();
    }
  }

  private Optional<Object> fetchStepParameters(String key) {
    if (nodeExecution == null || !entityTypes.contains(NodeExecutionEntityType.STEP_PARAMETERS)) {
      return Optional.empty();
    }
    return ExpressionEvaluatorUtils.fetchField(nodeExecutionsCache.extractFinalStepParameters(nodeExecution), key);
  }

  private Optional<Object> fetchOutcomeOrOutput(String key) {
    if (nodeExecution == null
        || (!entityTypes.contains(NodeExecutionEntityType.OUTCOME)
            && !entityTypes.contains(NodeExecutionEntityType.SWEEPING_OUTPUT))) {
      return Optional.empty();
    }

    Ambiance newAmbiance = nodeExecution.getAmbiance();
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
      return Optional.ofNullable(outcomeService.resolve(newAmbiance, RefObjectUtil.getOutcomeRefObject(key)));
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
          pmsSweepingOutputService.resolve(newAmbiance, RefObjectUtil.getSweepingOutputRefObject(key)));
    } catch (SweepingOutputException ignored) {
      return Optional.empty();
    }
  }
}

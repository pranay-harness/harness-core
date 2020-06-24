package io.harness.engine.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionSweepingOutputInstance;
import io.harness.beans.SweepingOutput;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.references.RefObject;
import io.harness.resolvers.ResolverUtils;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@OwnedBy(CDC)
@Redesign
@Singleton
public class ExecutionSweepingOutputServiceImpl implements ExecutionSweepingOutputService {
  @Inject private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject private Injector injector;
  @Inject private ExecutionSweepingOutputInstanceRepository repository;

  @Override
  public String consumeInternal(Ambiance ambiance, String name, SweepingOutput value, int levelsToKeep) {
    if (levelsToKeep >= 0) {
      ambiance = ambiance.clone(levelsToKeep);
    }

    try {
      ExecutionSweepingOutputInstance instance =
          repository.save(ExecutionSweepingOutputInstance.builder()
                              .uuid(generateUuid())
                              .planExecutionId(ambiance.getPlanExecutionId())
                              .levels(ambiance.getLevels())
                              .name(name)
                              .value(value)
                              .levelRuntimeIdIdx(ResolverUtils.prepareLevelRuntimeIdIdx(ambiance.getLevels()))
                              .build());
      return instance.getUuid();
    } catch (DuplicateKeyException ex) {
      throw new SweepingOutputException(format("Sweeping output with name %s is already saved", name), ex);
    }
  }

  @Override
  public SweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    if (!refObject.getName().contains(".")) {
      // It is not an expression-like ref-object.
      return resolveUsingRuntimeId(ambiance, refObject);
    }

    EngineExpressionEvaluator evaluator =
        expressionEvaluatorProvider.get(null, ambiance, EnumSet.of(NodeExecutionEntityType.SWEEPING_OUTPUT), true);
    injector.injectMembers(evaluator);
    Object value = evaluator.evaluateExpression(EngineExpressionEvaluator.createExpression(refObject.getName()));
    return (value instanceof SweepingOutput) ? (SweepingOutput) value : null;
  }

  private SweepingOutput resolveUsingRuntimeId(Ambiance ambiance, RefObject refObject) {
    String name = refObject.getName();
    List<ExecutionSweepingOutputInstance> instances = repository.findByPlanExecutionIdAndNameAndLevelRuntimeIdIdxIn(
        ambiance.getPlanExecutionId(), name, ResolverUtils.prepareLevelRuntimeIdIndices(ambiance));
    // Multiple instances might be returned if the same name was saved at different levels/specificity.
    ExecutionSweepingOutputInstance instance = EmptyPredicate.isEmpty(instances)
        ? null
        : instances.stream()
              .max(Comparator.comparing(ExecutionSweepingOutputInstance::getLevelRuntimeIdIdx))
              .orElse(null);
    if (instance == null) {
      throw new SweepingOutputException(format("Could not resolve sweeping output with name '%s'", name));
    }

    return instance.getValue();
  }
}

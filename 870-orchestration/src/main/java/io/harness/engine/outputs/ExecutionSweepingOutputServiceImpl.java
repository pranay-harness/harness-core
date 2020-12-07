package io.harness.engine.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.data.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.sdk.core.resolver.ResolverUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(CDC)
@Redesign
@Singleton
public class ExecutionSweepingOutputServiceImpl implements ExecutionSweepingOutputService {
  @Inject private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject private Injector injector;
  @Inject private MongoTemplate mongoTemplate;

  @Override
  public String consumeInternal(Ambiance ambiance, String name, SweepingOutput value, int levelsToKeep) {
    if (levelsToKeep >= 0) {
      ambiance = AmbianceUtils.clone(ambiance, levelsToKeep);
    }

    try {
      ExecutionSweepingOutputInstance instance =
          mongoTemplate.insert(ExecutionSweepingOutputInstance.builder()
                                   .uuid(generateUuid())
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .levels(ambiance.getLevelsList())
                                   .name(name)
                                   .value(convertToDocument(value))
                                   .levelRuntimeIdIdx(ResolverUtils.prepareLevelRuntimeIdIdx(ambiance.getLevelsList()))
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
    Query query = query(where(ExecutionSweepingOutputKeys.planExecutionId).is(ambiance.getPlanExecutionId()))
                      .addCriteria(where(ExecutionSweepingOutputKeys.name).is(name))
                      .addCriteria(where(ExecutionSweepingOutputKeys.levelRuntimeIdIdx)
                                       .in(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance)));
    List<ExecutionSweepingOutputInstance> instances = mongoTemplate.find(query, ExecutionSweepingOutputInstance.class);
    // Multiple instances might be returned if the same name was saved at different levels/specificity.
    ExecutionSweepingOutputInstance instance = EmptyPredicate.isEmpty(instances)
        ? null
        : instances.stream()
              .max(Comparator.comparing(ExecutionSweepingOutputInstance::getLevelRuntimeIdIdx))
              .orElse(null);
    if (instance == null) {
      throw new SweepingOutputException(format("Could not resolve sweeping output with name '%s'", name));
    }

    return convertToObject(instance.getValue());
  }
}

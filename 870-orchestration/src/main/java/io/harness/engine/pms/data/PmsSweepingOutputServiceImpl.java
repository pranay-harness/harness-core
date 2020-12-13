package io.harness.engine.pms.data;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.data.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.engine.outputs.SweepingOutputException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.resolver.ResolverUtils;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mongodb.DuplicateKeyException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class PmsSweepingOutputServiceImpl implements PmsSweepingOutputService {
  @Inject private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject private Injector injector;
  @Inject private MongoTemplate mongoTemplate;

  @Override
  public String resolve(Ambiance ambiance, RefObject refObject) {
    if (!refObject.getName().contains(".")) {
      // It is not an expression-like ref-object.
      return resolveUsingRuntimeId(ambiance, refObject);
    }

    EngineExpressionEvaluator evaluator =
        expressionEvaluatorProvider.get(null, ambiance, EnumSet.of(NodeExecutionEntityType.SWEEPING_OUTPUT), true);
    injector.injectMembers(evaluator);
    Object value = evaluator.evaluateExpression(EngineExpressionEvaluator.createExpression(refObject.getName()));
    return (String) value;
  }

  private String resolveUsingRuntimeId(Ambiance ambiance, RefObject refObject) {
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

    return instance.getValue() == null ? null : instance.getValue().toJson();
  }

  @Override
  public String consumeInternal(Ambiance ambiance, String name, String value, int levelsToKeep) {
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
                                   .value(DocumentOrchestrationUtils.convertToDocumentFromJson(value))
                                   .levelRuntimeIdIdx(ResolverUtils.prepareLevelRuntimeIdIdx(ambiance.getLevelsList()))
                                   .build());
      return instance.getUuid();
    } catch (DuplicateKeyException ex) {
      throw new SweepingOutputException(format("Sweeping output with name %s is already saved", name), ex);
    }
  }
}

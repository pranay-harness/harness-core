package io.harness.engine.outcomes;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.OutcomeInstance;
import io.harness.data.OutcomeInstance.OutcomeInstanceKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.ResolverUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(CDC)
@Redesign
@Singleton
public class OutcomeServiceImpl implements OutcomeService {
  @Inject private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject private Injector injector;
  @Inject private MongoTemplate mongoTemplate;

  @Override
  public String consumeInternal(Ambiance ambiance, String name, Outcome value, int levelsToKeep) {
    Level producedBy = AmbianceUtils.obtainCurrentLevel(ambiance);
    if (levelsToKeep >= 0) {
      ambiance = AmbianceUtils.clone(ambiance, levelsToKeep);
    }

    try {
      OutcomeInstance instance =
          mongoTemplate.insert(OutcomeInstance.builder()
                                   .uuid(generateUuid())
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .levels(ambiance.getLevelsList())
                                   .producedBy(producedBy)
                                   .name(name)
                                   .outcome(convertToDocument(value))
                                   .levelRuntimeIdIdx(ResolverUtils.prepareLevelRuntimeIdIdx(ambiance.getLevelsList()))
                                   .build());
      return instance.getUuid();
    } catch (DuplicateKeyException ex) {
      throw new OutcomeException(format("Outcome with name %s is already saved", name), ex);
    }
  }

  @Override
  public Outcome resolve(Ambiance ambiance, RefObject refObject) {
    if (EmptyPredicate.isNotEmpty(refObject.getProducerId())) {
      return resolveUsingProducerSetupId(ambiance, refObject);
    }
    if (!refObject.getName().contains(".")) {
      // It is not an expression-like ref-object.
      return resolveUsingRuntimeId(ambiance, refObject);
    }

    EngineExpressionEvaluator evaluator =
        expressionEvaluatorProvider.get(null, ambiance, EnumSet.of(NodeExecutionEntityType.OUTCOME), true);
    injector.injectMembers(evaluator);
    Object value = evaluator.evaluateExpression(EngineExpressionEvaluator.createExpression(refObject.getName()));
    return (value instanceof Outcome) ? (Outcome) value : null;
  }

  private Outcome resolveUsingProducerSetupId(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
    String name = refObject.getName();

    Query query = query(where(OutcomeInstanceKeys.planExecutionId).is(ambiance.getPlanExecutionId()))
                      .addCriteria(where(OutcomeInstanceKeys.name).is(name))
                      .addCriteria(where(OutcomeInstanceKeys.producedBySetupId).is(refObject.getProducerId()))
                      .with(Sort.by(Direction.DESC, OutcomeInstanceKeys.createdAt));

    List<OutcomeInstance> instances = mongoTemplate.find(query, OutcomeInstance.class);

    // Multiple instances might be returned if the same plan node executed multiple times.
    if (EmptyPredicate.isEmpty(instances)) {
      throw new OutcomeException(format("Could not resolve outcome with name '%s'", name));
    }
    return convertToObject(instances.get(0).getOutcome());
  }

  private Outcome resolveUsingRuntimeId(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
    String name = refObject.getName();
    Query query =
        query(where(OutcomeInstanceKeys.planExecutionId).is(ambiance.getPlanExecutionId()))
            .addCriteria(where(OutcomeInstanceKeys.name).is(name))
            .addCriteria(
                where(OutcomeInstanceKeys.levelRuntimeIdIdx).in(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance)));

    List<OutcomeInstance> instances = mongoTemplate.find(query, OutcomeInstance.class);

    // Multiple instances might be returned if the same name was saved at different levels/specificity.
    OutcomeInstance instance = EmptyPredicate.isEmpty(instances)
        ? null
        : instances.stream().max(Comparator.comparing(OutcomeInstance::getLevelRuntimeIdIdx)).orElse(null);
    if (instance == null) {
      throw new OutcomeException(format("Could not resolve outcome with name '%s'", name));
    }
    return convertToObject(instance.getOutcome());
  }

  @Override
  public List<Outcome> fetchOutcomes(List<String> outcomeInstanceIds) {
    if (isEmpty(outcomeInstanceIds)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    Query query = query(where(OutcomeInstanceKeys.uuid).in(outcomeInstanceIds));
    Iterable<OutcomeInstance> outcomesInstances = mongoTemplate.find(query, OutcomeInstance.class);
    for (OutcomeInstance instance : outcomesInstances) {
      outcomes.add(convertToObject(instance.getOutcome()));
    }
    return outcomes;
  }

  @Override
  public Outcome fetchOutcome(@NonNull String outcomeInstanceId) {
    Query query = query(where(OutcomeInstanceKeys.uuid).is(outcomeInstanceId));
    Optional<OutcomeInstance> outcomeInstance =
        Optional.ofNullable(mongoTemplate.findOne(query, OutcomeInstance.class));
    return outcomeInstance.map(oi -> convertToObject(oi.getOutcome())).orElse(null);
  }

  @Override
  public List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId) {
    Query query = query(where(OutcomeInstanceKeys.planExecutionId).is(planExecutionId))
                      .addCriteria(where(OutcomeInstanceKeys.producedByRuntimeId).is(runtimeId))
                      .with(Sort.by(Direction.DESC, OutcomeInstanceKeys.createdAt));

    List<OutcomeInstance> outcomeInstances = mongoTemplate.find(query, OutcomeInstance.class);
    if (isEmpty(outcomeInstances)) {
      return Collections.emptyList();
    }

    return outcomeInstances.stream().map(oi -> convertToObject(oi.getOutcome())).collect(Collectors.toList());
  }
}

package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DuplicateKeyException;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.ambiance.Level.LevelKeys;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
import io.harness.data.OutcomeInstance.OutcomeInstanceKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.EngineAmbianceExpressionEvaluator;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.engine.services.OutcomeException;
import io.harness.engine.services.OutcomeService;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.persistence.HPersistence;
import io.harness.references.RefObject;
import io.harness.resolvers.ResolverUtils;
import org.mongodb.morphia.query.Sort;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@Singleton
public class OutcomeServiceImpl implements OutcomeService {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private Injector injector;

  @Override
  public String consumeInternal(Ambiance ambiance, String name, Outcome value, int levelsToKeep) {
    Level producedBy = ambiance.obtainCurrentLevel();
    if (levelsToKeep >= 0) {
      ambiance = ambiance.clone(levelsToKeep);
    }

    try {
      return hPersistence.save(OutcomeInstance.builder()
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .levels(ambiance.getLevels())
                                   .producedBy(producedBy)
                                   .name(name)
                                   .outcome(value)
                                   .build());
    } catch (DuplicateKeyException ex) {
      throw new OutcomeException(format("Outcome with name %s is already saved", name), ex);
    }
  }

  @Override
  public Outcome resolve(Ambiance ambiance, RefObject refObject) {
    if (EmptyPredicate.isNotEmpty(refObject.getProducerId())) {
      return resolveUsingProducerId(ambiance, refObject);
    }
    if (!refObject.getName().contains(".")) {
      // It is not an expression-like ref-object.
      return resolveUsingRuntimeId(ambiance, refObject);
    }

    EngineAmbianceExpressionEvaluator evaluator = EngineAmbianceExpressionEvaluator.builder()
                                                      .ambiance(ambiance)
                                                      .entityTypes(EnumSet.of(NodeExecutionEntityType.OUTCOME))
                                                      .refObjectSpecific(true)
                                                      .build();
    injector.injectMembers(evaluator);
    Object value = evaluator.evaluateExpression(EngineExpressionEvaluator.createExpression(refObject.getName()));
    return (value instanceof Outcome) ? (Outcome) value : null;
  }

  private Outcome resolveUsingProducerId(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
    String name = refObject.getName();
    List<OutcomeInstance> instances =
        hPersistence.createQuery(OutcomeInstance.class, excludeAuthority)
            .filter(OutcomeInstanceKeys.planExecutionId, ambiance.getPlanExecutionId())
            .filter(OutcomeInstanceKeys.name, name)
            .filter(OutcomeInstanceKeys.producedBy + "." + LevelKeys.setupId, refObject.getProducerId())
            .order(Sort.descending(OutcomeInstanceKeys.createdAt))
            .asList();

    // Multiple instances might be returned if the same plan node executed multiple times.
    if (EmptyPredicate.isEmpty(instances)) {
      throw new OutcomeException(format("Could not resolve outcome with name '%s'", name));
    }
    return instances.get(0).getOutcome();
  }

  private Outcome resolveUsingRuntimeId(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
    String name = refObject.getName();
    List<OutcomeInstance> instances = hPersistence.createQuery(OutcomeInstance.class, excludeAuthority)
                                          .filter(OutcomeInstanceKeys.planExecutionId, ambiance.getPlanExecutionId())
                                          .filter(OutcomeInstanceKeys.name, name)
                                          .field(OutcomeInstanceKeys.levelRuntimeIdIdx)
                                          .in(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance))
                                          .asList();

    // Multiple instances might be returned if the same name was saved at different levels/specificity.
    OutcomeInstance instance = EmptyPredicate.isEmpty(instances)
        ? null
        : instances.stream().max(Comparator.comparing(OutcomeInstance::getLevelRuntimeIdIdx)).orElse(null);
    if (instance == null) {
      throw new OutcomeException(format("Could not resolve outcome with name '%s'", name));
    }
    return instance.getOutcome();
  }

  @Override
  public List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId) {
    List<OutcomeInstance> outcomeInstances = hPersistence.createQuery(OutcomeInstance.class, excludeAuthority)
                                                 .filter(OutcomeInstanceKeys.planExecutionId, planExecutionId)
                                                 .filter(OutcomeInstanceKeys.runtimeId, runtimeId)
                                                 .asList();

    if (isEmpty(outcomeInstances)) {
      return Collections.emptyList();
    }

    return outcomeInstances.stream().map(OutcomeInstance::getOutcome).collect(Collectors.toList());
  }
}

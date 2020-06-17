package io.harness.engine.outcomes;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.EngineAmbianceExpressionEvaluator;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.references.RefObject;
import io.harness.resolvers.ResolverUtils;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@Singleton
public class OutcomeServiceImpl implements OutcomeService {
  @Inject private Injector injector;
  @Inject private OutcomeRepository outcomeRepository;

  @Override
  public String consumeInternal(Ambiance ambiance, String name, Outcome value, int levelsToKeep) {
    Level producedBy = ambiance.obtainCurrentLevel();
    if (levelsToKeep >= 0) {
      ambiance = ambiance.clone(levelsToKeep);
    }

    try {
      OutcomeInstance instance =
          outcomeRepository.save(OutcomeInstance.builder()
                                     .planExecutionId(ambiance.getPlanExecutionId())
                                     .levels(ambiance.getLevels())
                                     .producedBy(producedBy)
                                     .name(name)
                                     .outcome(value)
                                     .levelRuntimeIdIdx(ResolverUtils.prepareLevelRuntimeIdIdx(ambiance.getLevels()))
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

    EngineAmbianceExpressionEvaluator evaluator = EngineAmbianceExpressionEvaluator.builder()
                                                      .ambiance(ambiance)
                                                      .entityTypes(EnumSet.of(NodeExecutionEntityType.OUTCOME))
                                                      .refObjectSpecific(true)
                                                      .build();
    injector.injectMembers(evaluator);
    Object value = evaluator.evaluateExpression(EngineExpressionEvaluator.createExpression(refObject.getName()));
    return (value instanceof Outcome) ? (Outcome) value : null;
  }

  private Outcome resolveUsingProducerSetupId(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
    String name = refObject.getName();
    List<OutcomeInstance> instances =
        outcomeRepository.findByPlanExecutionIdAndNameAndProducedBySetupIdOrderByCreatedAtDesc(
            ambiance.getPlanExecutionId(), name, refObject.getProducerId());
    // Multiple instances might be returned if the same plan node executed multiple times.
    if (EmptyPredicate.isEmpty(instances)) {
      throw new OutcomeException(format("Could not resolve outcome with name '%s'", name));
    }
    return instances.get(0).getOutcome();
  }

  private Outcome resolveUsingRuntimeId(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
    String name = refObject.getName();

    List<OutcomeInstance> instances = outcomeRepository.findByPlanExecutionIdAndNameAndLevelRuntimeIdIdxIn(
        ambiance.getPlanExecutionId(), name, ResolverUtils.prepareLevelRuntimeIdIndices(ambiance));

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
  public List<Outcome> fetchOutcomes(List<String> outcomeInstanceIds) {
    if (isEmpty(outcomeInstanceIds)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    Iterable<OutcomeInstance> outcomesInstances = outcomeRepository.findAllById(outcomeInstanceIds);
    for (OutcomeInstance instance : outcomesInstances) {
      outcomes.add(instance.getOutcome());
    }
    return outcomes;
  }

  @Override
  public Outcome fetchOutcome(@NonNull String outcomeInstanceId) {
    Optional<OutcomeInstance> outcomeInstance = outcomeRepository.findById(outcomeInstanceId);
    return outcomeInstance.map(OutcomeInstance::getOutcome).orElse(null);
  }

  @Override
  public List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId) {
    List<OutcomeInstance> outcomeInstances =
        outcomeRepository.findByPlanExecutionIdAndProducedByRuntimeIdOrderByCreatedAtDesc(planExecutionId, runtimeId);

    if (isEmpty(outcomeInstances)) {
      return Collections.emptyList();
    }

    return outcomeInstances.stream().map(OutcomeInstance::getOutcome).collect(Collectors.toList());
  }
}

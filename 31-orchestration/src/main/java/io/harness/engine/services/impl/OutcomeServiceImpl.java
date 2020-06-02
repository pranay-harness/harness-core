package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;

import com.google.inject.Inject;
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
import io.harness.engine.services.OutcomeException;
import io.harness.engine.services.OutcomeService;
import io.harness.persistence.HPersistence;
import io.harness.references.RefObject;
import io.harness.resolvers.ResolverUtils;
import org.mongodb.morphia.query.Sort;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
public class OutcomeServiceImpl implements OutcomeService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  @Override
  public String consumeInternal(Ambiance ambiance, String name, Outcome value, int levelsToKeep) {
    Level producedBy = ambiance.obtainCurrentLevel();
    if (levelsToKeep >= 0) {
      ambiance = ambiance.cloneForFinish(levelsToKeep);
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
    return resolveUsingScope(ambiance, refObject);
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

  private Outcome resolveUsingScope(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
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
  public Optional<Outcome> find(Ambiance ambiance, String setupId, String runtimeId, String name) {
    OutcomeInstance instance = hPersistence.createQuery(OutcomeInstance.class, excludeAuthority)
                                   .filter(OutcomeInstanceKeys.planExecutionId, ambiance.getPlanExecutionId())
                                   .filter(OutcomeInstanceKeys.name, name)
                                   .filter(OutcomeInstanceKeys.producedBy + "." + LevelKeys.setupId, setupId)
                                   .filter(OutcomeInstanceKeys.producedBy + "." + LevelKeys.runtimeId, runtimeId)
                                   .order(Sort.descending(OutcomeInstanceKeys.createdAt))
                                   .get();
    return Optional.ofNullable(instance).map(OutcomeInstance::getOutcome);
  }
}

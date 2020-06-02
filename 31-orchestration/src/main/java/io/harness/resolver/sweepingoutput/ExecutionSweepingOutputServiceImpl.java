package io.harness.resolver.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DuplicateKeyException;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.data.structure.EmptyPredicate;
import io.harness.persistence.HPersistence;
import io.harness.references.RefObject;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import io.harness.resolvers.ResolverUtils;

import java.util.Comparator;
import java.util.List;

@OwnedBy(CDC)
@Redesign
@Singleton
public class ExecutionSweepingOutputServiceImpl implements ExecutionSweepingOutputService {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;

  @Override
  public String consumeInternal(Ambiance ambiance, String name, SweepingOutput value, int levelsToKeep) {
    if (levelsToKeep >= 0) {
      ambiance = ambiance.cloneForFinish(levelsToKeep);
    }

    try {
      return hPersistence.save(ExecutionSweepingOutputInstance.builder()
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .levels(ambiance.getLevels())
                                   .name(name)
                                   .value(value)
                                   .build());
    } catch (DuplicateKeyException ex) {
      throw new SweepingOutputException(format("Sweeping output with name %s is already saved", name), ex);
    }
  }

  @Override
  public SweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    String name = refObject.getName();
    List<ExecutionSweepingOutputInstance> instances =
        hPersistence.createQuery(ExecutionSweepingOutputInstance.class, excludeAuthority)
            .filter(ExecutionSweepingOutputKeys.planExecutionId, ambiance.getPlanExecutionId())
            .filter(ExecutionSweepingOutputKeys.name, name)
            .field(ExecutionSweepingOutputKeys.levelRuntimeIdIdx)
            .in(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance))
            .asList();

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

package io.harness.resolver.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DuplicateKeyException;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Ambiance.AmbianceKeys;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.references.RefObject;
import io.harness.references.RefType;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import io.harness.resolvers.Resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@Singleton
public class ExecutionSweepingOutputResolver implements Resolver<SweepingOutput> {
  public static final String RESOLVER_TYPE = RefType.SWEEPING_OUTPUT;

  @Inject @Named("enginePersistence") private HPersistence hPersistence;

  @Override
  public SweepingOutput consume(@NotNull Ambiance ambiance, @NotNull String name, @NotNull SweepingOutput value) {
    try {
      hPersistence.save(ExecutionSweepingOutputInstance.builder().ambiance(ambiance).name(name).value(value).build());
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException(format("Sweeping output with name %s is already saved", name), ex);
    }

    return value;
  }

  @Override
  public SweepingOutput resolve(@NotNull Ambiance ambiance, @NotNull RefObject refObject) {
    String name = refObject.getName();
    List<ExecutionSweepingOutputInstance> instances =
        hPersistence.createQuery(ExecutionSweepingOutputInstance.class, excludeAuthority)
            .filter(ExecutionSweepingOutputKeys.ambiance + "." + AmbianceKeys.planExecutionId,
                ambiance.getPlanExecutionId())
            .filter(ExecutionSweepingOutputKeys.name, name)
            .field(ExecutionSweepingOutputKeys.levelRuntimeIdIdx)
            .in(prepareLevelRuntimeIdIndices(ambiance))
            .asList();

    // Multiple instances might be returned if the same name was saved at different levels/specificity.
    ExecutionSweepingOutputInstance instance = EmptyPredicate.isEmpty(instances)
        ? null
        : instances.stream()
              .max(Comparator.comparing(ExecutionSweepingOutputInstance::getLevelRuntimeIdIdx))
              .orElse(null);
    if (instance == null) {
      throw new InvalidRequestException(format("Could not resolve sweeping output with name '%s'", name));
    }

    return instance.getValue();
  }

  private List<String> prepareLevelRuntimeIdIndices(@NotNull Ambiance ambiance) {
    if (EmptyPredicate.isEmpty(ambiance.getLevels())) {
      // If the ambiance has no level executions, the instance also shouldn't have any level executions.
      return Collections.singletonList("");
    }

    List<String> levelRuntimeIdIndices = new ArrayList<>();
    levelRuntimeIdIndices.add("");
    for (int i = 1; i <= ambiance.getLevels().size(); i++) {
      levelRuntimeIdIndices.add(
          ExecutionSweepingOutputInstance.prepareLevelRuntimeIdIdx(ambiance.getLevels().subList(0, i)));
    }
    return levelRuntimeIdIndices;
  }

  @Override
  public RefType getType() {
    return RefType.builder().type(RESOLVER_TYPE).build();
  }
}

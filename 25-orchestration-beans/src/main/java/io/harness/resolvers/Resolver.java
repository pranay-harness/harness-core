package io.harness.resolvers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.references.RefObject;
import io.harness.registries.RegistrableEntity;
import io.harness.state.io.StepTransput;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
public interface Resolver<T extends StepTransput> extends RegistrableEntity {
  T resolve(@NotNull Ambiance ambiance, @NotNull RefObject refObject);

  String consumeInternal(@NotNull Ambiance ambiance, @NotNull String name, T value, int levelsToKeep);

  default String consume(@NotNull Ambiance ambiance, @NotNull String name, T value, String groupName) {
    if (EmptyPredicate.isEmpty(groupName)) {
      return consumeInternal(ambiance, name, value, -1);
    }
    if (groupName.equals(ResolverUtils.GLOBAL_GROUP_SCOPE)) {
      return consumeInternal(ambiance, name, value, 0);
    }

    if (EmptyPredicate.isEmpty(ambiance.getLevels())) {
      throw new GroupNotFoundException(groupName);
    }

    List<Level> levels = ambiance.getLevels();
    for (int i = levels.size() - 1; i >= 0; i--) {
      Level level = levels.get(i);
      if (groupName.equals(level.getStepType().getGroup())) {
        return consumeInternal(ambiance, name, value, i + 1);
      }
    }

    throw new GroupNotFoundException(groupName);
  }
}

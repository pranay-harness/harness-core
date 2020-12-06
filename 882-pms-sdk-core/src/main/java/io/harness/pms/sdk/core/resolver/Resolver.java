package io.harness.pms.sdk.core.resolver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.data.StepTransput;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface Resolver<T extends StepTransput> {
  T resolve(@NotNull Ambiance ambiance, @NotNull RefObject refObject);

  String consumeInternal(@NotNull Ambiance ambiance, @NotNull String name, T value, int levelsToKeep);

  default String consume(@NotNull Ambiance ambiance, @NotNull String name, T value, String groupName) {
    if (EmptyPredicate.isEmpty(groupName)) {
      return consumeInternal(ambiance, name, value, -1);
    }
    if (groupName.equals(ResolverUtils.GLOBAL_GROUP_SCOPE)) {
      return consumeInternal(ambiance, name, value, 0);
    }

    if (EmptyPredicate.isEmpty(ambiance.getLevelsList())) {
      throw new GroupNotFoundException(groupName);
    }

    List<Level> levels = ambiance.getLevelsList();
    for (int i = levels.size() - 1; i >= 0; i--) {
      Level level = levels.get(i);
      if (groupName.equals(level.getGroup())) {
        return consumeInternal(ambiance, name, value, i + 1);
      }
    }

    throw new GroupNotFoundException(groupName);
  }
}

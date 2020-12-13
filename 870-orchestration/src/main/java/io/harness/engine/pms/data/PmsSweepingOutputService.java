package io.harness.engine.pms.data;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.resolver.GroupNotFoundException;
import io.harness.pms.sdk.core.resolver.ResolverUtils;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface PmsSweepingOutputService {
  String resolve(Ambiance ambiance, RefObject refObject);

  default String consume(@NotNull Ambiance ambiance, @NotNull String name, String value, String groupName) {
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

  String consumeInternal(@NotNull Ambiance ambiance, @NotNull String name, String value, int levelsToKeep);
}

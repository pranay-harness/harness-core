package io.harness.engine.pms.data;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.execution.utils.AmbianceUtils;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface Resolver {
  String resolve(Ambiance ambiance, RefObject refObject);

  default String consume(@NotNull Ambiance ambiance, @NotNull String name, String value, String groupName) {
    Level producedBy = AmbianceUtils.obtainCurrentLevel(ambiance);
    if (EmptyPredicate.isEmpty(groupName)) {
      return consumeInternal(ambiance, producedBy, name, value, groupName);
    }
    if (groupName.equals(ResolverUtils.GLOBAL_GROUP_SCOPE)) {
      return consumeInternal(AmbianceUtils.clone(ambiance, 0), producedBy, name, value, groupName);
    }

    if (EmptyPredicate.isEmpty(ambiance.getLevelsList())) {
      throw new GroupNotFoundException(groupName);
    }

    List<Level> levels = ambiance.getLevelsList();
    for (int i = levels.size() - 1; i >= 0; i--) {
      Level level = levels.get(i);
      if (groupName.equals(level.getGroup())) {
        return consumeInternal(AmbianceUtils.clone(ambiance, i + 1), producedBy, name, value, groupName);
      }
    }

    throw new GroupNotFoundException(groupName);
  }

  String consumeInternal(
      @NotNull Ambiance ambiance, Level producedBy, @NotNull String name, String value, String groupName);
}

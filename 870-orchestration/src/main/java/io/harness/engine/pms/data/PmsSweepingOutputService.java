package io.harness.engine.pms.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.resolver.GroupNotFoundException;
import io.harness.pms.sdk.core.resolver.ResolverUtils;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PmsSweepingOutputService {
  String resolve(Ambiance ambiance, RefObject refObject);

  default String consume(@NotNull Ambiance ambiance, @NotNull String name, String value, String groupName) {
    if (EmptyPredicate.isEmpty(groupName)) {
      return consumeInternal(ambiance, name, value, -1, groupName);
    }
    if (groupName.equals(ResolverUtils.GLOBAL_GROUP_SCOPE)) {
      return consumeInternal(ambiance, name, value, 0, groupName);
    }

    if (EmptyPredicate.isEmpty(ambiance.getLevelsList())) {
      throw new GroupNotFoundException(groupName);
    }

    List<Level> levels = ambiance.getLevelsList();
    for (int i = levels.size() - 1; i >= 0; i--) {
      Level level = levels.get(i);
      if (groupName.equals(level.getGroup())) {
        return consumeInternal(ambiance, name, value, i + 1, groupName);
      }
    }

    throw new GroupNotFoundException(groupName);
  }

  RawOptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject);

  String consumeInternal(
      @NotNull Ambiance ambiance, @NotNull String name, String value, int levelsToKeep, String groupName);

  List<RawOptionalSweepingOutput> findOutputsUsingNodeId(Ambiance ambiance, String name, List<String> nodeIds);
}

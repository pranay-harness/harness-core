package io.harness.engine.pms.data;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.resolver.GroupNotFoundException;
import io.harness.pms.sdk.core.resolver.ResolverUtils;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.NonNull;

public interface PmsOutcomeService {
  String resolve(Ambiance ambiance, RefObject refObject);

  default String consume(
      @NotNull Ambiance ambiance, @NotNull String name, String value, String groupName, boolean isGraphOutcome) {
    if (hasNone(groupName)) {
      return consumeInternal(ambiance, name, value, -1, isGraphOutcome);
    }
    if (groupName.equals(ResolverUtils.GLOBAL_GROUP_SCOPE)) {
      return consumeInternal(ambiance, name, value, 0, isGraphOutcome);
    }

    if (hasNone(ambiance.getLevelsList())) {
      throw new GroupNotFoundException(groupName);
    }

    List<Level> levels = ambiance.getLevelsList();
    for (int i = levels.size() - 1; i >= 0; i--) {
      Level level = levels.get(i);
      if (groupName.equals(level.getGroup())) {
        return consumeInternal(ambiance, name, value, i + 1, isGraphOutcome);
      }
    }

    throw new GroupNotFoundException(groupName);
  }

  String consumeInternal(
      @NotNull Ambiance ambiance, @NotNull String name, String value, int levelsToKeep, boolean isGraphOutcome);

  List<String> findAllByRuntimeId(String planExecutionId, String runtimeId);

  List<String> findAllByRuntimeId(String planExecutionId, String runtimeId, boolean isGraphOutcome);

  List<String> fetchOutcomes(List<String> outcomeInstanceIds);

  String fetchOutcome(@NonNull String outcomeInstanceId);

  OptionalOutcome resolveOptional(Ambiance ambiance, RefObject refObject);
}

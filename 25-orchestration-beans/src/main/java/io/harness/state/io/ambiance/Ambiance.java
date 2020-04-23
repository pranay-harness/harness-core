package io.harness.state.io.ambiance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.collect.ImmutableMap;

import io.harness.annotations.Redesign;
import io.harness.logging.AutoLogContext;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Redesign
@Value
@Builder
public class Ambiance {
  // Setup details accountId, appId
  @Singular Map<String, String> setupAbstractions;

  // These is a combination of setup/execution Id for a particular level
  @Builder.Default @NonFinal List<Level> levels = new ArrayList<>();

  @NotNull String executionInstanceId;

  public AutoLogContext autoLogContext() {
    ImmutableMap.Builder<String, String> logContext = ImmutableMap.builder();
    logContext.putAll(setupAbstractions);
    levels.forEach(level -> logContext.put(level.getLevelKey() + "ExecutionId", level.getRuntimeId()));
    return new AutoLogContext(logContext.build(), OVERRIDE_ERROR);
  }

  public Ambiance deepCopy() {
    return KryoUtils.clone(this);
  }

  public void addLevel(@Valid @NotNull Level level) {
    int existingIndex = getExistingIndex(level);
    if (existingIndex > -1) {
      levels.subList(existingIndex, levels.size()).clear();
    }
    levels.add(level);
  }

  private int getExistingIndex(@NotNull @Valid Level level) {
    int existingIndex = -1;
    int idx = 0;
    for (Level existingLevel : levels) {
      if (existingLevel.getLevelKey().equals(level.getLevelKey())) {
        existingIndex = idx;
        break;
      }
      idx++;
    }
    return existingIndex;
  }

  public String getCurrentRuntimeId() {
    if (isEmpty(levels)) {
      return null;
    }
    return levels.get(levels.size() - 1).getRuntimeId();
  }
}
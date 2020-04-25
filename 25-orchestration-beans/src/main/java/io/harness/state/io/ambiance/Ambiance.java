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
  @Builder.Default @NonFinal List<LevelExecution> levelExecutions = new ArrayList<>();

  @NotNull String executionInstanceId;

  public AutoLogContext autoLogContext() {
    ImmutableMap.Builder<String, String> logContext = ImmutableMap.builder();
    logContext.putAll(setupAbstractions);
    levelExecutions.forEach(level -> logContext.put(level.getLevelKey() + "ExecutionId", level.getRuntimeId()));
    return new AutoLogContext(logContext.build(), OVERRIDE_ERROR);
  }

  public Ambiance deepCopy() {
    return KryoUtils.clone(this);
  }

  public void addLevel(@Valid @NotNull LevelExecution levelExecution) {
    int existingIndex = getExistingIndex(levelExecution);
    if (existingIndex > -1) {
      levelExecutions.subList(existingIndex, levelExecutions.size()).clear();
    }
    levelExecutions.add(levelExecution);
  }

  private int getExistingIndex(@NotNull @Valid LevelExecution levelExecution) {
    int existingIndex = -1;
    int idx = 0;
    for (LevelExecution existingLevelExecution : levelExecutions) {
      if (existingLevelExecution.getLevelKey().equals(levelExecution.getLevelKey())) {
        existingIndex = idx;
        break;
      }
      idx++;
    }
    return existingIndex;
  }

  public String getCurrentRuntimeId() {
    if (isEmpty(levelExecutions)) {
      return null;
    }
    return levelExecutions.get(levelExecutions.size() - 1).getRuntimeId();
  }
}
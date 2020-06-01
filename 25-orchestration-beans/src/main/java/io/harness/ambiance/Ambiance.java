package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.annotations.VisibleForTesting;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.logging.AutoLogContext;
import io.harness.plan.input.InputArgs;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "AmbianceKeys")
@EqualsAndHashCode
public class Ambiance {
  @Getter @NotNull InputArgs inputArgs;
  @Getter @NonFinal List<Level> levels;
  @Getter @NotNull String planExecutionId;

  @Builder
  public Ambiance(InputArgs inputArgs, List<Level> levels, String planExecutionId) {
    this.inputArgs = inputArgs;
    this.levels = levels == null ? new ArrayList<>() : levels;
    this.planExecutionId = planExecutionId;
  }

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = inputArgs == null ? new HashMap<>() : new HashMap<>(inputArgs.strMap());
    logContext.put(AmbianceKeys.planExecutionId, planExecutionId);
    levels.forEach(level -> {
      logContext.put("identifier", level.getIdentifier());
      logContext.put("runtimeId", level.getRuntimeId());
      logContext.put("setupId", level.getSetupId());
    });
    return new AutoLogContext(logContext, OVERRIDE_ERROR);
  }

  public void addLevel(@Valid @NotNull Level level) {
    levels.add(level);
  }

  public Ambiance cloneForFinish() {
    Ambiance cloned = deepCopy();
    cloned.levels.remove(levels.size() - 1);
    return cloned;
  }

  public Ambiance cloneForFinish(int levelsToKeep) {
    Ambiance cloned = deepCopy();
    if (levelsToKeep >= 0 && levelsToKeep < levels.size()) {
      cloned.levels.subList(levelsToKeep, cloned.levels.size()).clear();
    }
    return cloned;
  }

  public Ambiance cloneForChild() {
    return deepCopy();
  }

  public String obtainCurrentRuntimeId() {
    Level level = obtainCurrentLevel();
    return level == null ? null : level.getRuntimeId();
  }

  public Level obtainCurrentLevel() {
    if (isEmpty(levels)) {
      return null;
    }
    return levels.get(levels.size() - 1);
  }

  @VisibleForTesting
  Ambiance deepCopy() {
    Kryo kryo = new Kryo();
    return kryo.copy(this);
  }

  public static Ambiance fromExecutionInstances(
      @NotNull PlanExecution planExecution, @NotNull NodeExecution nodeExecution) {
    return Ambiance.builder()
        .inputArgs(planExecution.getInputArgs())
        .planExecutionId(nodeExecution.getPlanExecutionId())
        .levels(nodeExecution.getLevels())
        .build();
  }
}

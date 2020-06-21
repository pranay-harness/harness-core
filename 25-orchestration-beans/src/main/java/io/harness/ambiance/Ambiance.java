package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import com.google.common.annotations.VisibleForTesting;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.logging.AutoLogContext;
import io.harness.plan.input.InputArgs;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@FieldNameConstants(innerTypeName = "AmbianceKeys")
@Getter
@EqualsAndHashCode
public class Ambiance {
  @NotNull InputArgs inputArgs;
  @NotNull List<Level> levels;
  @NotNull String planExecutionId;

  @Setter @Transient int expressionFunctorToken;

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

    // Execution engine starts
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }

  public void addLevel(@Valid @NotNull Level level) {
    levels.add(level);
  }

  public Ambiance cloneForFinish() {
    return clone(levels.size() - 1);
  }

  public Ambiance cloneForChild() {
    return clone(levels.size());
  }

  public Ambiance clone(int levelsToKeep) {
    Ambiance cloned = deepCopy();
    if (levelsToKeep >= 0 && levelsToKeep < levels.size()) {
      cloned.levels.subList(levelsToKeep, cloned.levels.size()).clear();
    }
    return cloned;
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
    return KryoUtils.clone(this);
  }

  public static Ambiance fromNodeExecution(@NotNull InputArgs inputArgs, @NotNull NodeExecution nodeExecution) {
    return Ambiance.builder()
        .inputArgs(inputArgs)
        .planExecutionId(nodeExecution.getPlanExecutionId())
        .levels(nodeExecution.getLevels())
        .build();
  }
}

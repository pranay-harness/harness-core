package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.ambiance.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Redesign
@FieldNameConstants(innerTypeName = "AmbianceKeys")
@Getter
@EqualsAndHashCode
public class Ambiance {
  @NotNull Map<String, String> setupAbstractions;
  @NotNull List<Level> levels;
  @NotNull String planExecutionId;

  @Setter private transient int expressionFunctorToken;

  @Builder
  public Ambiance(Map<String, String> setupAbstractions, List<Level> levels, String planExecutionId) {
    this.setupAbstractions = setupAbstractions;
    this.levels = levels == null ? new ArrayList<>() : levels;
    this.planExecutionId = planExecutionId;
  }

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }

  public Map<String, String> logContextMap() {
    Map<String, String> logContext = setupAbstractions == null ? new HashMap<>() : new HashMap<>(setupAbstractions);
    logContext.put(AmbianceKeys.planExecutionId, planExecutionId);
    Level level = obtainCurrentLevel();
    if (level != null) {
      logContext.put("identifier", level.getIdentifier());
      logContext.put("runtimeId", level.getRuntimeId());
      logContext.put("setupId", level.getSetupId());
    }
    return logContext;
  }

  public void addLevel(@Valid @NotNull Level level) {
    levels.add(level);
  }

  public String obtainCurrentRuntimeId() {
    Level level = obtainCurrentLevel();
    return level == null || isEmpty(level.getRuntimeId()) ? null : level.getRuntimeId();
  }

  public Level obtainCurrentLevel() {
    if (isEmpty(levels)) {
      return null;
    }
    return levels.get(levels.size() - 1);
  }
}

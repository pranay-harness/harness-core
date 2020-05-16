package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
@FieldNameConstants(innerTypeName = "AmbianceKeys")
public class Ambiance {
  // Setup details accountId, appId
  @Singular Map<String, String> setupAbstractions;

  // These is a combination of setup/execution Id for a particular level
  @Builder.Default @NonFinal List<LevelExecution> levelExecutions = new ArrayList<>();

  @NotNull String planExecutionId;

  public AutoLogContext autoLogContext() {
    ImmutableMap.Builder<String, String> logContext = ImmutableMap.builder();
    logContext.putAll(setupAbstractions);
    logContext.put(AmbianceKeys.planExecutionId, planExecutionId);
    levelExecutions.forEach(level -> {
      logContext.put(level.getIdentifier() + "ExecutionId", level.getRuntimeId());
      logContext.put(level.getIdentifier() + "SetupId", level.getSetupId());
    });
    return new AutoLogContext(logContext.build(), OVERRIDE_ERROR);
  }

  public void addLevelExecution(@Valid @NotNull LevelExecution levelExecution) {
    levelExecutions.add(levelExecution);
  }

  public Ambiance cloneForFinish() {
    Ambiance cloned = deepCopy();
    cloned.levelExecutions.remove(levelExecutions.size() - 1);
    return cloned;
  }

  public Ambiance cloneForChild() {
    return deepCopy();
  }

  public String obtainCurrentRuntimeId() {
    LevelExecution levelExecution = obtainCurrentLevelExecution();
    return levelExecution == null ? null : levelExecution.getRuntimeId();
  }

  public LevelExecution obtainCurrentLevelExecution() {
    if (isEmpty(levelExecutions)) {
      return null;
    }
    return levelExecutions.get(levelExecutions.size() - 1);
  }

  @VisibleForTesting
  public Ambiance deepCopy() {
    return KryoUtils.clone(this);
  }
}
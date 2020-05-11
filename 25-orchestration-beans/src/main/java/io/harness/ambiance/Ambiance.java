package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static org.apache.commons.lang3.StringUtils.SPACE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
      logContext.put(level.getLevelName() + "ExecutionId", level.getRuntimeId());
      logContext.put(level.getLevelName() + "SetupId", level.getSetupId());
    });
    return new AutoLogContext(logContext.build(), OVERRIDE_ERROR);
  }

  public void addLevelExecution(@Valid @NotNull LevelExecution levelExecution) {
    Level targetLevel = levelExecution.getLevel();
    levelExecutions = levelExecutions.stream()
                          .filter(ex -> ex.getLevelPriority() < targetLevel.getOrder())
                          .collect(Collectors.toList());
    levelExecutions.add(levelExecution);
  }

  public Ambiance cloneForFinish(@NonNull @Valid Level upcomingLevel) {
    Ambiance cloned = deepCopy();
    Level finishedLevel = obtainCurrentLevel();
    if (finishedLevel == null) {
      throw new InvalidRequestException("Finished Level Cannot be null");
    }
    Level cleanerLevel = finishedLevel.getOrder() <= upcomingLevel.getOrder() ? finishedLevel : upcomingLevel;
    cloned.levelExecutions = cloned.levelExecutions.stream()
                                 .filter(ex -> ex.getLevelPriority() < cleanerLevel.getOrder())
                                 .collect(Collectors.toList());
    return cloned;
  }

  public Ambiance cloneForChild(@NonNull @Valid Level childLevel) {
    Ambiance cloned = deepCopy();
    Level parentLevel = obtainCurrentLevel();
    if (parentLevel == null) {
      throw new InvalidRequestException("Parent Level Cannot be null");
    }
    if (parentLevel.getOrder() > childLevel.getOrder()) {
      throw new InvalidRequestException(HarnessStringUtils.join(SPACE, "Parent Level cannot have order",
          String.valueOf(parentLevel.getOrder()), "greater than the child", String.valueOf(childLevel.getOrder())));
    }
    return cloned;
  }

  public Level obtainCurrentLevel() {
    LevelExecution levelExecution = obtainCurrentLevelExecution();
    return levelExecution == null ? null : levelExecution.getLevel();
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
  Ambiance deepCopy() {
    return KryoUtils.clone(this);
  }
}
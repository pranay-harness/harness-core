package io.harness.pms.execution.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.plan.execution.SetupAbstractionKeys;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class AmbianceUtils {
  public static Ambiance cloneForFinish(@NonNull Ambiance ambiance) {
    return clone(ambiance, ambiance.getLevelsList().size() - 1);
  }

  public static Ambiance cloneForChild(@NonNull Ambiance ambiance) {
    return clone(ambiance, ambiance.getLevelsList().size());
  }

  public static Ambiance cloneForChild(@NonNull Ambiance ambiance, @NonNull Level level) {
    Ambiance.Builder builder = cloneBuilder(ambiance, ambiance.getLevelsList().size());
    return builder.addLevels(level).build();
  }

  public static Ambiance.Builder cloneBuilder(Ambiance ambiance, int levelsToKeep) {
    return clone(ambiance, levelsToKeep).toBuilder();
  }

  public static Ambiance clone(Ambiance ambiance, int levelsToKeep) {
    Ambiance.Builder clonedBuilder = ambiance.toBuilder().clone();
    if (levelsToKeep >= 0 && levelsToKeep < ambiance.getLevelsList().size()) {
      List<Level> clonedLevels = clonedBuilder.getLevelsList().subList(0, levelsToKeep);
      clonedBuilder.clearLevels();
      clonedBuilder.addAllLevels(clonedLevels);
    }
    return clonedBuilder.build();
  }

  @VisibleForTesting
  static Ambiance deepCopy(Ambiance ambiance) throws InvalidProtocolBufferException {
    return Ambiance.parseFrom(ambiance.toByteString());
  }

  public static String obtainCurrentRuntimeId(Ambiance ambiance) {
    Level level = obtainCurrentLevel(ambiance);
    return level == null || isEmpty(level.getRuntimeId()) ? null : level.getRuntimeId();
  }

  public static String obtainCurrentSetupId(Ambiance ambiance) {
    Level level = obtainCurrentLevel(ambiance);
    return level == null || isEmpty(level.getRuntimeId()) ? null : level.getSetupId();
  }

  public static Level obtainCurrentLevel(Ambiance ambiance) {
    if (isEmpty(ambiance.getLevelsList())) {
      return null;
    }
    return ambiance.getLevelsList().get(ambiance.getLevelsList().size() - 1);
  }

  public static String obtainStepIdentifier(Ambiance ambiance) {
    Level level = obtainCurrentLevel(ambiance);
    return level == null || isEmpty(level.getIdentifier()) ? null : level.getIdentifier();
  }

  public static AutoLogContext autoLogContext(Ambiance ambiance) {
    return new AutoLogContext(logContextMap(ambiance), OVERRIDE_NESTS);
  }

  public static Map<String, String> logContextMap(Ambiance ambiance) {
    Map<String, String> logContext = ambiance.getSetupAbstractionsMap() == null
        ? new HashMap<>()
        : new HashMap<>(ambiance.getSetupAbstractionsMap());
    logContext.put("planExecutionId", ambiance.getPlanExecutionId());
    Level level = obtainCurrentLevel(ambiance);
    if (level != null) {
      logContext.put("identifier", level.getIdentifier());
      logContext.put("runtimeId", level.getRuntimeId());
      logContext.put("setupId", level.getSetupId());
      logContext.put("stepType", level.getStepType().getType());
    }
    return logContext;
  }

  public static String getAccountId(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get(SetupAbstractionKeys.accountId);
  }

  public static String getProjectIdentifier(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get(SetupAbstractionKeys.projectIdentifier);
  }

  public static String getOrgIdentifier(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get(SetupAbstractionKeys.orgIdentifier);
  }

  public static StepType getCurrentStepType(Ambiance ambiance) {
    Level level = obtainCurrentLevel(ambiance);
    return level == null || level.getStepType() == null ? null : level.getStepType();
  }

  public static String getCurrentGroup(Ambiance ambiance) {
    Level level = obtainCurrentLevel(ambiance);
    return level == null || level.getGroup() == null ? null : level.getGroup();
  }

  public static long getCurrentLevelStartTs(Ambiance ambiance) {
    Level currLevel = obtainCurrentLevel(ambiance);
    if (currLevel == null) {
      throw new InvalidRequestException("Ambiance.levels is empty");
    }
    return currLevel.getStartTs();
  }
}

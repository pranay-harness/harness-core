package io.harness.state.io.ambiance;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.collect.ImmutableMap;

import io.harness.annotations.Redesign;
import io.harness.logging.AutoLogContext;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;
import javax.validation.constraints.NotNull;

@Redesign
@Value
@Builder
public class Ambiance {
  // Setup details accountId, appId
  @Singular Map<String, String> setupAbstractions;

  // These is a combination of setup/execution Id for a particular level
  @Singular Map<String, Level> levels;

  @NotNull String executionInstanceId;

  public AutoLogContext autoLogContext() {
    ImmutableMap.Builder<String, String> logContext = ImmutableMap.builder();
    logContext.putAll(setupAbstractions);
    levels.forEach((key, level) -> logContext.put(key + "ExecutionId", level.getRuntimeId()));
    return new AutoLogContext(logContext.build(), OVERRIDE_ERROR);
  }

  public AmbianceBuilder cloneBuilder() {
    return Ambiance.builder()
        .setupAbstractions(setupAbstractions)
        .levels(levels)
        .executionInstanceId(executionInstanceId);
  }
}
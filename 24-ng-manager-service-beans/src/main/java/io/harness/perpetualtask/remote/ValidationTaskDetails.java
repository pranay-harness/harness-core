package io.harness.perpetualtask.remote;

import io.harness.delegate.beans.TaskData;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class ValidationTaskDetails {
  @NotNull String accountId;
  Map<String, String> setupAbstractions;
  @NotNull TaskData taskData;
}

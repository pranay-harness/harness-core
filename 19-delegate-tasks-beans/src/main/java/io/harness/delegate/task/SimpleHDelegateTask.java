package io.harness.delegate.task;

import io.harness.delegate.beans.TaskData;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.Map;

@Value
@Builder
public class SimpleHDelegateTask implements HDelegateTask {
  @NonNull String accountId;
  @NonNull TaskData data;
  @Singular Map<String, String> setupAbstractions;
  String uuid;
  LinkedHashMap<String, String> logStreamingAbstractions;
}

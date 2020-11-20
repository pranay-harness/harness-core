package io.harness.delegate.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.TaskData;
import io.harness.tasks.Task;

import java.util.LinkedHashMap;
import java.util.Map;

@OwnedBy(CDC)
public interface HDelegateTask extends Task {
  String getAccountId();
  Map<String, String> getSetupAbstractions();
  TaskData getData();
  LinkedHashMap<String, String> getLogStreamingAbstractions();
}

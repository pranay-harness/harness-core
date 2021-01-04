package io.harness.selection.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchDelegateSelectionLog {
  private String taskId;
  private DelegateSelectionLogTaskMetadata taskMetadata;

  @Builder.Default private List<DelegateSelectionLog> delegateSelectionLogs = new ArrayList<>();

  public void append(DelegateSelectionLog delegateSelectionLog) {
    Optional<DelegateSelectionLog> logs = delegateSelectionLogs.stream()
                                              .filter(log
                                                  -> log.getAccountId().equals(delegateSelectionLog.getAccountId())
                                                      && log.getTaskId().equals(delegateSelectionLog.getTaskId())
                                                      && log.getMessage().equals(delegateSelectionLog.getMessage()))
                                              .findFirst();
    if (logs.isPresent()) {
      logs.get().getDelegateIds().addAll(delegateSelectionLog.getDelegateIds());
      logs.get().getDelegateMetadata().putAll(delegateSelectionLog.getDelegateMetadata());
    } else {
      delegateSelectionLogs.add(delegateSelectionLog);
    }
  }
}

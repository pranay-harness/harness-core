package io.harness.selection.log;

import com.google.common.collect.MoreCollectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchDelegateSelectionLog {
  private final String taskId;
  private final DelegateSelectionLogTaskMetadata taskMetadata;

  @Builder.Default private final List<DelegateSelectionLog> delegateSelectionLogs = new ArrayList<>();

  public void append(final DelegateSelectionLog delegateSelectionLog) {
    final Optional<DelegateSelectionLog> logs =
        delegateSelectionLogs.stream()
            .filter(log
                -> log.getAccountId().equals(delegateSelectionLog.getAccountId())
                    && log.getTaskId().equals(delegateSelectionLog.getTaskId())
                    && log.getMessage().equals(delegateSelectionLog.getMessage()))
            .collect(MoreCollectors.toOptional());
    if (logs.isPresent()) {
      logs.get().getDelegateIds().addAll(delegateSelectionLog.getDelegateIds());
      logs.get().getDelegateMetadata().putAll(delegateSelectionLog.getDelegateMetadata());
    } else {
      delegateSelectionLogs.add(delegateSelectionLog);
    }
  }
}

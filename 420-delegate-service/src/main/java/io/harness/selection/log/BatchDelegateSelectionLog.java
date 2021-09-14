/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  private final boolean isTaskNg;
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

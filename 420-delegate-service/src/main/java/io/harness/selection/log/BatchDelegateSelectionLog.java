/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

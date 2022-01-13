/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.persistence.PersistenceWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class WaitInstanceService {
  @Inject private PersistenceWrapper persistenceWrapper;
  public static final Duration MAX_CALLBACK_PROCESSING_TIME = Duration.ofMinutes(1);

  public WaitInstance fetchForProcessingWaitInstance(String waitInstanceId, long now) {
    return persistenceWrapper.fetchForProcessingWaitInstance(waitInstanceId, now);
  }

  public ProgressUpdate fetchForProcessingProgressUpdate(Set<String> busyCorrelationIds, long now) {
    return persistenceWrapper.fetchForProcessingProgressUpdate(busyCorrelationIds, now);
  }

  public void checkProcessingTime(long startTime) {
    final long passed = System.currentTimeMillis() - startTime;
    if (passed > MAX_CALLBACK_PROCESSING_TIME.toMillis()) {
      log.error("It took more than {} ms before we processed the callback. THIS IS VERY BAD!!!",
          MAX_CALLBACK_PROCESSING_TIME.toMillis());
    }
  }
}

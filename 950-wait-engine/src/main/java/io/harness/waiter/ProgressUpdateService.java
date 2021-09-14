/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ProgressData;
import io.harness.waiter.persistence.PersistenceWrapper;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class ProgressUpdateService implements Runnable {
  @Inject private Injector injector;
  @Inject private PersistenceWrapper persistenceWrapper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private WaitInstanceService waitInstanceService;

  @Override
  public void run() {
    Set<String> busyCorrelationIds = new HashSet<>();
    while (true) {
      try {
        final long now = System.currentTimeMillis();
        ProgressUpdate progressUpdate = waitInstanceService.fetchForProcessingProgressUpdate(busyCorrelationIds, now);
        if (progressUpdate == null) {
          break;
        }

        if (progressUpdate.getExpireProcessing() > now) {
          continue;
        }
        log.info("Starting to process progress response");

        ProgressData progressData = (ProgressData) kryoSerializer.asInflatedObject(progressUpdate.getProgressData());

        List<WaitInstance> waitInstances = persistenceWrapper.fetchWaitInstances(progressUpdate.getCorrelationId());
        for (WaitInstance waitInstance : waitInstances) {
          ProgressCallback progressCallback = waitInstance.getProgressCallback();
          injector.injectMembers(progressCallback);
          progressCallback.notify(progressUpdate.getCorrelationId(), progressData);
        }
        log.info("Processed progress response");
        persistenceWrapper.delete(progressUpdate);
      } catch (Exception e) {
        log.error("Exception occurred while running progress service", e);
      }
    }
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.scheduler;

import io.harness.lock.PersistentLocker;
import io.harness.lock.noop.PersistentNoopLocker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.Getter;

@Singleton
public class BackgroundSchedulerLocker {
  @Getter private PersistentLocker locker;

  @Inject
  public BackgroundSchedulerLocker(
      PersistentLocker persistentLocker, @Named("BackgroundSchedule") SchedulerConfig configuration) {
    locker = configuration.isClustered() ? new PersistentNoopLocker() : persistentLocker;
  }
}

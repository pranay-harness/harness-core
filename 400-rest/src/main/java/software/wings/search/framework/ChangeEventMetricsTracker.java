/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Getter
@Slf4j
class ChangeEventMetricsTracker {
  private ConcurrentMap<String, Double> runningAverageTime = new ConcurrentHashMap<>();
  private ConcurrentMap<String, Long> numChangeEvents = new ConcurrentHashMap<>();

  void updateAverage(String key, double timeTaken) {
    double averageVal = runningAverageTime.getOrDefault(key, (double) 0);
    double n = numChangeEvents.getOrDefault(key, (long) 0);
    averageVal = (averageVal * (n / (n + 1))) + (timeTaken / (n + 1));
    runningAverageTime.put(key, averageVal);
    numChangeEvents.put(key, (long) n + 1);
  }
}

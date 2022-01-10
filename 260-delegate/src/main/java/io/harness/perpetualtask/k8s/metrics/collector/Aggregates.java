/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.metrics.collector;

import static com.google.common.base.Preconditions.checkState;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.grpc.utils.HTimestamps;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import lombok.Getter;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
class Aggregates {
  @Getter private Aggregate cpu;
  @Getter private Aggregate memory;

  @Getter private Aggregate storageCapacity;
  @Getter private Aggregate storageUsed;

  private String startTimestamp;
  private String lastTimestamp;
  private final Duration minWindow;

  Aggregates(Duration minWindow) {
    this.minWindow = minWindow;
    this.cpu = new Aggregate();
    this.memory = new Aggregate();

    this.storageCapacity = new Aggregate();
    this.storageUsed = new Aggregate();
  }

  static class Aggregate {
    @Getter private long max;
    private long sum;
    private int count;

    private void update(long value) {
      max = Math.max(value, this.max);
      sum += value;
      count++;
    }

    long getAverage() {
      checkState(count != 0);
      return sum / count;
    }
  }

  void update(long cpuNano, long memoryBytes, String timestamp) {
    if (timestamp != null && !timestamp.equals(this.lastTimestamp)) {
      cpu.update(cpuNano);
      memory.update(memoryBytes);
      this.lastTimestamp = timestamp;
      if (this.startTimestamp == null) {
        this.startTimestamp = timestamp;
      }
    }
  }

  void updateStorage(long storageCapacityBytes, long storageUsedBytes, String timestamp) {
    if (timestamp != null && !timestamp.equals(this.lastTimestamp)) {
      storageCapacity.update(storageCapacityBytes);
      storageUsed.update(storageUsedBytes);
      this.lastTimestamp = timestamp;
      if (this.startTimestamp == null) {
        this.startTimestamp = timestamp;
      }
    }
  }

  Timestamp getAggregateTimestamp() {
    return HTimestamps.parse(startTimestamp);
  }

  Duration getAggregateWindow() {
    Timestamp start = HTimestamps.parse(startTimestamp);
    Timestamp last = HTimestamps.parse(lastTimestamp);
    return Durations.add(minWindow, Timestamps.between(start, last));
  }
}

package io.harness.perpetualtask.k8s.metrics.collector;

import static com.google.common.base.Preconditions.checkState;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;

import io.harness.grpc.utils.HTimestamps;
import lombok.Getter;

class Aggregates {
  @Getter private Aggregate cpu;
  @Getter private Aggregate memory;
  private String startTimestamp;
  private String lastTimestamp;
  private final Duration minWindow;

  Aggregates(Duration minWindow) {
    this.minWindow = minWindow;
    this.cpu = new Aggregate();
    this.memory = new Aggregate();
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

  Timestamp getAggregateTimestamp() {
    return HTimestamps.parse(startTimestamp);
  }

  Duration getAggregateWindow() {
    Timestamp start = HTimestamps.parse(startTimestamp);
    Timestamp last = HTimestamps.parse(lastTimestamp);
    return Durations.add(minWindow, Timestamps.between(start, last));
  }
}

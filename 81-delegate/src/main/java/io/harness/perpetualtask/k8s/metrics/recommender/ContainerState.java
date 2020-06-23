package io.harness.perpetualtask.k8s.metrics.recommender;

import io.harness.histogram.DecayingHistogram;
import io.harness.histogram.ExponentialHistogramOptions;
import io.harness.histogram.Histogram;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
public class ContainerState {
  private static final double MIN_SAMPLE_WEIGHT = 0.1;
  private static final double EPSILON = 0.001 * MIN_SAMPLE_WEIGHT;
  private static final double BUCKET_GROWTH_RATIO = 1.05;
  private static final Duration CPU_HALF_LIFE = Duration.ofHours(24);

  private final String namespace;
  private final String podName;
  private final String containerName;

  /*
  Only the peak memory for the 24h period should be added to the histogram. For cpu, we should add all samples.
   */
  private long memoryPeak;
  private final Histogram cpuHistogram;
  private Instant firstSampleStart;
  private Instant lastSampleStart;
  private int totalSamplesCount;

  public ContainerState(String namespace, String podName, String containerName) {
    this.namespace = namespace;
    this.podName = podName;
    this.containerName = containerName;
    this.cpuHistogram = new DecayingHistogram(
        new ExponentialHistogramOptions(1000.0, 0.01, BUCKET_GROWTH_RATIO, EPSILON), CPU_HALF_LIFE);
  }

  public void addCpuSample(long podCpuNano, String timestamp) {
    Instant now = Instant.parse(timestamp);
    if (lastSampleStart != null && !now.isAfter(lastSampleStart)) {
      // skip duplicate or out-of-order
      return;
    }
    cpuHistogram.addSample(podCpuNano, MIN_SAMPLE_WEIGHT, now);
    if (firstSampleStart == null) {
      firstSampleStart = now;
    }
    lastSampleStart = now;
    totalSamplesCount++;
  }

  public void addMemorySample(long podMemoryBytes) {
    memoryPeak = Math.max(memoryPeak, podMemoryBytes);
  }
}

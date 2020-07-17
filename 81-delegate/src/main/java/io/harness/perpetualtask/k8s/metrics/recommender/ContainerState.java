package io.harness.perpetualtask.k8s.metrics.recommender;

import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.MIN_SAMPLE_WEIGHT;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.newCpuHistogram;

import io.harness.histogram.Histogram;
import lombok.Data;

import java.time.Instant;

@Data
public class ContainerState {
  private final String namespace;
  private final String podName;
  private final String containerName;

  /*
  Only the peak memory for the 24h period should be added to the histogram. For cpu, we should add all samples.
   */
  private long memoryPeak;
  private Instant memoryPeakTime;
  private final Histogram cpuHistogram;
  private Instant firstSampleStart;
  private Instant lastSampleStart;
  private int totalSamplesCount;

  public ContainerState(String namespace, String podName, String containerName) {
    this.namespace = namespace;
    this.podName = podName;
    this.containerName = containerName;
    this.cpuHistogram = newCpuHistogram();
  }

  public void addCpuSample(double containerCpuCores, String timestamp) {
    Instant now = Instant.parse(timestamp);
    if (lastSampleStart != null && !now.isAfter(lastSampleStart)) {
      // skip duplicate or out-of-order
      return;
    }
    cpuHistogram.addSample(containerCpuCores, MIN_SAMPLE_WEIGHT, now);
    if (firstSampleStart == null) {
      firstSampleStart = now;
    }
    lastSampleStart = now;
    totalSamplesCount++;
  }

  public void addMemorySample(long containerMemoryBytes, String timestamp) {
    if (containerMemoryBytes >= memoryPeak) {
      memoryPeak = containerMemoryBytes;
      memoryPeakTime = Instant.parse(timestamp);
    }
  }
}

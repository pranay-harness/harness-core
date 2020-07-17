package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.histogram.HistogramCheckpoint;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ContainerCheckpoint {
  Instant lastUpdateTime;
  HistogramCheckpoint cpuHistogram;
  HistogramCheckpoint memoryHistogram;

  Instant firstSampleStart;
  Instant lastSampleStart;
  int totalSamplesCount;

  long memoryPeak;
  Instant windowEnd;
  int version;
}

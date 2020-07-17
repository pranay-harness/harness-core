package io.harness.ccm.recommender.k8sworkload;

import io.harness.event.payloads.HistogramProto;
import io.harness.grpc.utils.HTimestamps;
import io.harness.histogram.DecayingHistogram;
import io.harness.histogram.ExponentialHistogramOptions;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.histogram.HistogramOptions;
import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class RecommenderUtils {
  // Change this when changing any of the parameters here, if the client & server side histograms
  // become incompatible (eg: if changing the histogram options)
  public static final int RECOMMENDER_VERSION = 1;

  // The minimal weight of any sample (prior to including decaying factor)
  public static final double MIN_SAMPLE_WEIGHT = 0.1;

  // The minimal weight kept in histograms, it should be small enough that old samples
  // (just inside MEMORY_AGGREGATION_WINDOW_LENGTH) added with MIN_SAMPLE_WEIGHT are still kept
  public static final double EPSILON = 0.001 * MIN_SAMPLE_WEIGHT;

  // The length of the memory usage history aggregated, which is 8 days.
  public static final Duration MEMORY_AGGREGATION_WINDOW_LENGTH = Duration.ofDays(8);

  // The length of a single interval, for which the peak memory usage is computed.
  // Memory usage peaks are aggregated in daily intervals. In other words there is one memory usage sample per day
  // (the maximum usage over that day).
  // Note: MEMORY_AGGREGATION_WINDOW_LENGTH must be integrally divisible by this value.
  public static final Duration MEMORY_AGGREGATION_INTERVAL = Duration.ofDays(1);

  // Defines the growth rate of the histogram buckets. Each bucket is wider than the previous one by this fraction.
  public static final double HISTOGRAM_BUCKET_SIZE_GROWTH = 0.05;

  // Options to be used by histograms that store CPU measures expressed in cores.
  public static final HistogramOptions CPU_HISTOGRAM_OPTIONS =
      new ExponentialHistogramOptions(1000.0, 0.01, 1 + HISTOGRAM_BUCKET_SIZE_GROWTH, EPSILON);

  // Options to be used by histograms that store memory measures expressed in bytes.
  public static final HistogramOptions MEMORY_HISTOGRAM_OPTIONS =
      new ExponentialHistogramOptions(1e12, 1e7, 1 + HISTOGRAM_BUCKET_SIZE_GROWTH, EPSILON);

  // The amount of time it takes a historical memory usage sample to lose half of its weight. In other words, a fresh
  // usage sample is twice as 'important' as one with age equal to the half life period.
  public static final Duration MEMORY_HISTOGRAM_DECAY_HALF_LIFE = Duration.ofDays(1);

  // The amount of time it takes a historical CPU usage sample to lose half of its weight.
  public static final Duration CPU_HISTOGRAM_DECAY_HALF_LIFE = Duration.ofDays(1);

  public static Histogram newCpuHistogram() {
    return new DecayingHistogram(CPU_HISTOGRAM_OPTIONS, CPU_HISTOGRAM_DECAY_HALF_LIFE);
  }

  public static Histogram newMemoryHistogram() {
    return new DecayingHistogram(MEMORY_HISTOGRAM_OPTIONS, MEMORY_HISTOGRAM_DECAY_HALF_LIFE);
  }

  public static HistogramProto checkpointToProto(HistogramCheckpoint histogramCheckpoint) {
    return HistogramProto.newBuilder()
        .setReferenceTimestamp(HTimestamps.fromInstant(histogramCheckpoint.getReferenceTimestamp()))
        .putAllBucketWeights(histogramCheckpoint.getBucketWeights())
        .setTotalWeight(histogramCheckpoint.getTotalWeight())
        .build();
  }

  public static HistogramCheckpoint protoToCheckpoint(HistogramProto histogramProto) {
    return HistogramCheckpoint.builder()
        .referenceTimestamp(HTimestamps.toInstant(histogramProto.getReferenceTimestamp()))
        .bucketWeights(histogramProto.getBucketWeightsMap())
        .totalWeight(histogramProto.getTotalWeight())
        .build();
  }
}

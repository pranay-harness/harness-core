/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.ccm.commons.entities.k8s.recommendation.PartialRecommendationHistogram;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;

import java.time.Instant;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PartialHistogramAggragator {
  public void aggregateInto(final Iterable<PartialRecommendationHistogram> partialRecommendationHistograms,
      final Map<String, Histogram> cpuHistograms, final Map<String, Histogram> memoryHistograms) {
    for (PartialRecommendationHistogram partialRecommendationHistogram : partialRecommendationHistograms) {
      Map<String, ContainerCheckpoint> containerCheckpoints = partialRecommendationHistogram.getContainerCheckpoints();
      for (Map.Entry<String, ContainerCheckpoint> stringContainerCheckpointEntry : containerCheckpoints.entrySet()) {
        String containerName = stringContainerCheckpointEntry.getKey();
        ContainerCheckpoint containerCheckpoint = stringContainerCheckpointEntry.getValue();

        // merge the day's cpu histogram into the aggregate cpu histogram
        HistogramCheckpoint cpuHistogramPartialCheckpoint = containerCheckpoint.getCpuHistogram();
        if (cpuHistogramPartialCheckpoint.getBucketWeights() != null) {
          Histogram cpuHistogramPartial = RecommenderUtils.loadFromCheckpointV2(cpuHistogramPartialCheckpoint);
          Histogram cpuHistogram = cpuHistograms.getOrDefault(containerName, RecommenderUtils.newCpuHistogramV2());
          cpuHistogram.merge(cpuHistogramPartial);
          cpuHistograms.put(containerName, cpuHistogram);
        }

        // add the day's memory peak into the aggregate memory histogram
        long memoryPeak = containerCheckpoint.getMemoryPeak();
        if (memoryPeak != 0) {
          Histogram memoryHistogram =
              memoryHistograms.getOrDefault(containerName, RecommenderUtils.newMemoryHistogramV2());
          memoryHistogram.addSample(memoryPeak, 1.0, Instant.EPOCH); // timestamp is irrelevant since no decay.
          memoryHistograms.put(containerName, memoryHistogram);
        }
      }
    }
  }
}

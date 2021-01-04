package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLContainerHistogramData {
  String containerName;
  QLHistogramExp cpuHistogram;
  QLHistogramExp memoryHistogram;
}

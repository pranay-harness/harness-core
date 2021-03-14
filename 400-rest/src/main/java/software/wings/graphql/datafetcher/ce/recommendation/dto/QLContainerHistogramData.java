package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._375_CE_GRAPHQL)
public class QLContainerHistogramData {
  String containerName;
  QLHistogramExp cpuHistogram;
  QLHistogramExp memoryHistogram;
}

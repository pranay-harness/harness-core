package io.harness.ccm.commons.beans.recommendation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendClusterRequest {
  Boolean allowBurst;
  Boolean allowOlderGen;
  List<String> category;
  List<String> excludes;
  List<String> includes;
  Long maxNodes;
  @Builder.Default Long minNodes = 3L;
  List<String> networkPerf;
  @Builder.Default Long onDemandPct = 100L;
  Boolean sameSize;
  Double sumCpu;
  @Builder.Default Long sumGpu = 0L;
  Double sumMem;
  String zone;
}

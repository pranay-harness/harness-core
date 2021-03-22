package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLContainerRecommendation implements QLObject {
  String containerName;
  QLResourceRequirement current;
  QLResourceRequirement burstable;
  QLResourceRequirement guaranteed;
  QLResourceRequirement recommended;
  //  requiredPercentiles 50, 80, 90, 95, 99
  QLResourceRequirement p50;
  QLResourceRequirement p80;
  QLResourceRequirement p90;
  QLResourceRequirement p95;
  QLResourceRequirement p99;
  int numDays;
  int totalSamplesCount;
}

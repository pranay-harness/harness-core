package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLContainerRecommendation implements QLObject {
  String containerName;
  QLResourceRequirement current;
  QLResourceRequirement burstable;
  QLResourceRequirement guaranteed;
  QLResourceRequirement recommended;
  int numDays;
  int totalSamplesCount;
}

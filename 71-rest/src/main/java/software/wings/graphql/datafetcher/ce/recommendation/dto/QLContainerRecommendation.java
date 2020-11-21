package software.wings.graphql.datafetcher.ce.recommendation.dto;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLContainerRecommendation implements QLObject {
  String containerName;
  QLResourceRequirement current;
  QLResourceRequirement burstable;
  QLResourceRequirement guaranteed;
  QLResourceRequirement recommended;
  int numDays;
  int totalSamplesCount;
}

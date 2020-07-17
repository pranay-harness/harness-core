package software.wings.graphql.datafetcher.ce.recommendation.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerRecommendation {
  String containerName;
  ResourceRequirement current;
  ResourceRequirement burstable;
  ResourceRequirement guaranteed;
  int numDays;
  int totalSamplesCount;
}

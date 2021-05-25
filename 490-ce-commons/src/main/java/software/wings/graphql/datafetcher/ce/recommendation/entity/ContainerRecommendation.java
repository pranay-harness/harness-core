package software.wings.graphql.datafetcher.ce.recommendation.entity;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerRecommendation {
  ResourceRequirement current;
  ResourceRequirement burstable;
  ResourceRequirement guaranteed;
  ResourceRequirement recommended;
  Map<String, ResourceRequirement> percentileBased;
  Cost lastDayCost;
  int numDays;
  int totalSamplesCount;
}

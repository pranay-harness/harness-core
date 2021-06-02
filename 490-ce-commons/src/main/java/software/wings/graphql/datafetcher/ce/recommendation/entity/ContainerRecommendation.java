package software.wings.graphql.datafetcher.ce.recommendation.entity;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerRecommendation {
  ResourceRequirement current;
  @Deprecated ResourceRequirement burstable;
  @Deprecated ResourceRequirement guaranteed;
  @Deprecated ResourceRequirement recommended;
  Map<String, ResourceRequirement> percentileBased;
  Cost lastDayCost;
  int numDays;
  int totalSamplesCount;
}

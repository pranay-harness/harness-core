package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
public class QLContainerRecommendation implements QLObject {
  String containerName;
  QLResourceRequirement current;
  QLResourceRequirement burstable;
  QLResourceRequirement guaranteed;
}

package software.wings.graphql.datafetcher.ce.recommendation.entity;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
public class ContainerRecommendation implements QLObject {
  String containerName;
  ResourceRequirement current;
  ResourceRequirement burstable;
  ResourceRequirement guaranteed;
}

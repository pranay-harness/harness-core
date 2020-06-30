package software.wings.graphql.datafetcher.ce.recommendation.entity;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ResourceRequirement {
  @Singular Map<String, String> requests;
  @Singular Map<String, String> limits;
}

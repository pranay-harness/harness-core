package software.wings.graphql.datafetcher.ce.recommendation.entity;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ResourceRequirement {
  public static final String MEMORY = "memory";
  public static final String CPU = "cpu";
  @Singular Map<String, String> requests;
  @Singular Map<String, String> limits;
}

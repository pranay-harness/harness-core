package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.data.structure.HasPredicate;
import io.harness.data.structure.HasPredicate.HasNone;

import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ResourceRequirement implements HasNone {
  public static final String MEMORY = "memory";
  public static final String CPU = "cpu";
  @Singular Map<String, String> requests;
  @Singular Map<String, String> limits;

  @Override
  public boolean hasNone() {
    return HasPredicate.hasNone(requests) && HasPredicate.hasNone(limits);
  }
}

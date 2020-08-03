package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.EmptyPredicate.IsEmpty;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ResourceRequirement implements IsEmpty {
  public static final String MEMORY = "memory";
  public static final String CPU = "cpu";
  @Singular Map<String, String> requests;
  @Singular Map<String, String> limits;

  @Override
  public boolean isEmpty() {
    return EmptyPredicate.isEmpty(requests) && EmptyPredicate.isEmpty(limits);
  }
}

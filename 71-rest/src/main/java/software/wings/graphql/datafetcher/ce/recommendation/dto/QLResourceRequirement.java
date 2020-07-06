package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;

import java.util.List;

@Value
@Builder
public class QLResourceRequirement implements QLObject {
  String yaml;
  @Singular List<QLResourceEntry> requests;
  @Singular List<QLResourceEntry> limits;
}

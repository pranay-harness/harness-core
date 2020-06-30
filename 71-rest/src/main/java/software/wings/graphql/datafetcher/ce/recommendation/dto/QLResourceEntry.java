package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Value;
import software.wings.graphql.schema.type.QLObject;

@Value(staticConstructor = "of")
public class QLResourceEntry implements QLObject {
  String name;
  String quantity;
}

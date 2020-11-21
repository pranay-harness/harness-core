package software.wings.graphql.datafetcher.ce.recommendation.dto;

import software.wings.graphql.schema.type.QLObject;

import lombok.Value;

@Value(staticConstructor = "of")
public class QLResourceEntry implements QLObject {
  String name;
  String quantity;
}

package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEnvironment implements QLObject {
  String id;
  String name;
  String description;

  // TODO: define type object
  String type;
}

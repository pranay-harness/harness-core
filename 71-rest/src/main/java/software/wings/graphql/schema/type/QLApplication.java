package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLApplication implements QLObject {
  String id;
  String name;
  String description;
}

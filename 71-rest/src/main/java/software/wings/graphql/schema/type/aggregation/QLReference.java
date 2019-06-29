package software.wings.graphql.schema.type.aggregation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLReference {
  String id;
  String name;
  String type;
}

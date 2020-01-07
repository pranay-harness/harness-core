package software.wings.graphql.schema.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QLApplicationInput {
  String name;
  String description;
}

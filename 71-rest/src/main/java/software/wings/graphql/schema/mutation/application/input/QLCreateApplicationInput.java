package software.wings.graphql.schema.mutation.application.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLCreateApplicationInput implements QLMutationInput {
  String requestId;
  String name;
  String description;
}

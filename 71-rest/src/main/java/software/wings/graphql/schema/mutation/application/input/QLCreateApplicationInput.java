package software.wings.graphql.schema.mutation.application.input;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLCreateApplicationInput implements QLMutationInput {
  String clientMutationId;
  String name;
  String description;
}

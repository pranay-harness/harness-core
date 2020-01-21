package software.wings.graphql.schema.type.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.schema.mutation.QLMutationInput;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class QLCreateUserInput implements QLMutationInput {
  String requestId;
  String name;
  String email;
}

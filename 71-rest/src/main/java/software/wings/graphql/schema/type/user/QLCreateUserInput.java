package software.wings.graphql.schema.type.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.schema.mutation.QLMutationInput;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class QLCreateUserInput implements QLMutationInput {
  private String clientMutationId;
  private String name;
  private String email;
  private List<String> userGroupIds;
}

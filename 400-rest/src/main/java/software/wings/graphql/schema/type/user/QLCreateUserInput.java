package software.wings.graphql.schema.type.user;

import io.harness.utils.RequestField;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class QLCreateUserInput implements QLMutationInput {
  private String clientMutationId;
  private String name;
  private String email;
  private RequestField<List<String>> userGroupIds;
}

package software.wings.graphql.schema.type.user;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
public class QLCreateUserPayload implements QLMutationPayload {
  private QLUser user;
  private String clientMutationId;
}

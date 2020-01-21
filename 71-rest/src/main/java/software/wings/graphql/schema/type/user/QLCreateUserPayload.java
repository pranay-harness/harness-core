package software.wings.graphql.schema.type.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
public class QLCreateUserPayload {
  private QLUser user;
  private String requestId;
}

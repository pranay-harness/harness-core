package software.wings.graphql.schema.type.secrets;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonDeserialize(as = QLWinRMCredential.class)
public interface QLSecret extends QLObject {
  String getId();
  String getName();
}

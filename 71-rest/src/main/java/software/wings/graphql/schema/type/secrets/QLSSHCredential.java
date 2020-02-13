package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHCredentialKeys")
public class QLSSHCredential implements QLSecret {
  String id;
  String name;
  QLSSHAuthenticationType authenticationType;
}

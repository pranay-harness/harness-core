package software.wings.graphql.schema.mutation.secretManager;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLHashicorpVaultAuthDetails {
  String authToken;
  String appRoleId;
  String secretId;
}

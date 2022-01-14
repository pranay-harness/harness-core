package io.harness.connector.entities.embedded.bitbucketconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePasswordApiAccess")
public class BitbucketUsernamePasswordApiAccess {
  String username;
  String usernameRef;
  String tokenRef;
}

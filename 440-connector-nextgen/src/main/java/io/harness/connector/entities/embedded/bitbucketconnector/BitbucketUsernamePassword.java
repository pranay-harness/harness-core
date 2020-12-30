package io.harness.connector.entities.embedded.bitbucketconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword")
public class BitbucketUsernamePassword implements BitbucketHttpAuth {
  String username;
  String usernameRef;
  String passwordRef;
}

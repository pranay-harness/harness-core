package io.harness.connector.entities.embedded.bitbucketconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketUsernamePassword")
@OwnedBy(DX)
public class BitbucketUsernamePassword implements BitbucketHttpAuth {
  String username;
  String usernameRef;
  String passwordRef;
}

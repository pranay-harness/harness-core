package io.harness.connector.entities.embedded.bitbucketconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication")
@OwnedBy(DX)
public class BitbucketHttpAuthentication implements BitbucketAuthentication {
  BitbucketHttpAuthenticationType type;
  BitbucketHttpAuth auth;
}

package io.harness.connector.entities.embedded.bitbucketconnector;

import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication")
public class BitbucketHttpAuthentication implements BitbucketAuthentication {
  BitbucketHttpAuthenticationType type;
  BitbucketHttpAuth auth;
}

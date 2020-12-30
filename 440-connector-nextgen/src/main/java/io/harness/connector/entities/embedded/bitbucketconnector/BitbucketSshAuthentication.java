package io.harness.connector.entities.embedded.bitbucketconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication")
public class BitbucketSshAuthentication implements BitbucketAuthentication {
  String sshKeyRef;
}

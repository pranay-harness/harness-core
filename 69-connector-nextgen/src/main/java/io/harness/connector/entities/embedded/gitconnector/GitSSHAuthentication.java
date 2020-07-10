package io.harness.connector.entities.embedded.gitconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("sshAuth")
public class GitSSHAuthentication implements GitAuthentication {
  String sshKeyReference;
}

package io.harness.connector.entities.embedded.gitconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gitconnector.GitSSHAuthentication")
public class GitSSHAuthentication implements GitAuthentication {
  String sshKeyReference;
}

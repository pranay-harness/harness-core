package io.harness.connector.entities.embedded.githubconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication")
public class GithubSshAuthentication implements GithubAuthentication {
  String sshKeyRef;
}

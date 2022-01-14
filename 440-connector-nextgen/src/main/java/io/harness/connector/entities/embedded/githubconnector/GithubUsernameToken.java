package io.harness.connector.entities.embedded.githubconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.githubconnector.GithubUsernameToken")
public class GithubUsernameToken implements GithubHttpAuth {
  String username;
  String usernameRef;
  String tokenRef;
}

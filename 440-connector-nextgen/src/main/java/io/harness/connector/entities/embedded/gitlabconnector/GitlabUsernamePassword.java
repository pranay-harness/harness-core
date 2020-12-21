package io.harness.connector.entities.embedded.gitlabconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gitlabconnector.GitlabUsernamePassword")
public class GitlabUsernamePassword implements GitlabHttpAuth {
  String username;
  String usernameRef;
  String passwordRef;
}

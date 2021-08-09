package io.harness.connector.entities.embedded.githubconnector;

import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication")
public class GithubHttpAuthentication implements GithubAuthentication {
  GithubHttpAuthenticationType type;
  GithubHttpAuth auth;
}

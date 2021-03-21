package io.harness.connector.entities.embedded.githubconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.githubconnector.GithubHttpAuthentication")
@OwnedBy(DX)
public class GithubHttpAuthentication implements GithubAuthentication {
  GithubHttpAuthenticationType type;
  GithubHttpAuth auth;
}

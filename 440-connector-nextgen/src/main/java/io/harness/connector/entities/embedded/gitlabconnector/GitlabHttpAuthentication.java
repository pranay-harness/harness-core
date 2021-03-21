package io.harness.connector.entities.embedded.gitlabconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication")
@OwnedBy(DX)
public class GitlabHttpAuthentication implements GitlabAuthentication {
  GitlabHttpAuthenticationType type;
  GitlabHttpAuth auth;
}

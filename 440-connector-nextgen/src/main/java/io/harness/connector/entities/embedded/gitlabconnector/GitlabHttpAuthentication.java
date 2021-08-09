package io.harness.connector.entities.embedded.gitlabconnector;

import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication")
public class GitlabHttpAuthentication implements GitlabAuthentication {
  GitlabHttpAuthenticationType type;
  GitlabHttpAuth auth;
}

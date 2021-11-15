package io.harness.delegate.beans.connector.scm.github;

import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.USERNAME_AND_PASSWORD;
import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.USERNAME_AND_TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubUsernamePassword.class, name = USERNAME_AND_PASSWORD)
  , @JsonSubTypes.Type(value = GithubUsernameToken.class, name = USERNAME_AND_TOKEN)
})
public interface GithubHttpCredentialsSpec extends DecryptableEntity {}

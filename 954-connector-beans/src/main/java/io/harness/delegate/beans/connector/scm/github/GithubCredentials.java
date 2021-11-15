package io.harness.delegate.beans.connector.scm.github;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubHttpCredentials.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = GithubSshCredentials.class, name = GitConfigConstants.SSH)
})
public interface GithubCredentials {}

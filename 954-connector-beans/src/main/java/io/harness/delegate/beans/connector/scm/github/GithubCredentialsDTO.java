package io.harness.delegate.beans.connector.scm.github;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubHttpCredentialsDTO.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = GithubSshCredentialsDTO.class, name = GitConfigConstants.SSH)
})
@Schema(name = "GithubCredentials", description = "This is a interface for details of the Github credentials")
public interface GithubCredentialsDTO {}

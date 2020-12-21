package io.harness.delegate.beans.connector.scm.github;

import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.GITHUB_APP;
import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.TOKEN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubAppSpecDTO.class, name = GITHUB_APP)
  , @JsonSubTypes.Type(value = GithubTokenSpecDTO.class, name = TOKEN)
})
public interface GithubApiAccessSpecDTO {}

package io.harness.delegate.beans.connector.scm.github;

import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.GITHUB_APP;
import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubAppSpec.class, name = GITHUB_APP)
  , @JsonSubTypes.Type(value = GithubTokenSpec.class, name = TOKEN)
})
public interface GithubApiAccessSpec extends DecryptableEntity {}

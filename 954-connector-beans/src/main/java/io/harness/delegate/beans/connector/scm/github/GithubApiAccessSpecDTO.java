package io.harness.delegate.beans.connector.scm.github;

import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.GITHUB_APP;
import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubAppSpecDTO.class, name = GITHUB_APP)
  , @JsonSubTypes.Type(value = GithubTokenSpecDTO.class, name = TOKEN)
})
@Schema(name = "GithubApiAccessSpec",
    description =
        "This contains details of the information such as references of username and password needed for Github API access")
public interface GithubApiAccessSpecDTO extends DecryptableEntity {}

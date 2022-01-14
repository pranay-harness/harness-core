package io.harness.delegate.beans.connector.scm.github;

import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.USERNAME_AND_PASSWORD;
import static io.harness.delegate.beans.connector.scm.github.GithubConnectorConstants.USERNAME_AND_TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubUsernamePasswordDTO.class, name = USERNAME_AND_PASSWORD)
  , @JsonSubTypes.Type(value = GithubUsernameTokenDTO.class, name = USERNAME_AND_TOKEN)
})
@Schema(name = "GithubHttpCredentialsSpec",
    description =
        "This is a interface for details of the Github credentials Specs such as references of username and password")
public interface GithubHttpCredentialsSpecDTO extends DecryptableEntity {}

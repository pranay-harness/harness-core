package io.harness.delegate.beans.connector.scm.gitlab;

import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_PASSWORD;
import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_TOKEN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitlabUsernamePasswordDTO.class, name = USERNAME_AND_PASSWORD)
  , @JsonSubTypes.Type(value = GitlabUsernameTokenDTO.class, name = USERNAME_AND_TOKEN)
})
public interface GitlabHttpCredentialsSpecDTO {}

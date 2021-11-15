package io.harness.delegate.beans.connector.scm.gitlab;

import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.KERBEROS;
import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_PASSWORD;
import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitlabUsernamePassword.class, name = USERNAME_AND_PASSWORD)
  , @JsonSubTypes.Type(value = GitlabUsernameToken.class, name = USERNAME_AND_TOKEN),
      @JsonSubTypes.Type(value = GitlabKerberos.class, name = KERBEROS)
})
public interface GitlabHttpCredentialsSpec extends DecryptableEntity {}

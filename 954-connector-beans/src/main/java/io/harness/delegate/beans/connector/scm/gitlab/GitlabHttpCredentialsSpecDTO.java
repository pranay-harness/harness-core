package io.harness.delegate.beans.connector.scm.gitlab;

import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.KERBEROS;
import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_PASSWORD;
import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitlabUsernamePasswordDTO.class, name = USERNAME_AND_PASSWORD)
  , @JsonSubTypes.Type(value = GitlabUsernameTokenDTO.class, name = USERNAME_AND_TOKEN),
      @JsonSubTypes.Type(value = GitlabKerberosDTO.class, name = KERBEROS)
})
@Schema(name = "GitlabHttpCredentialsSpec",
    description =
        "This is a interface for details of the Gitlab credentials Specs such as references of username and password")
public interface GitlabHttpCredentialsSpecDTO extends DecryptableEntity {}

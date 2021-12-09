package io.harness.delegate.beans.connector.scm.gitlab;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitlabHttpCredentialsDTO.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = GitlabSshCredentialsDTO.class, name = GitConfigConstants.SSH)
})
@Schema(name = "GitlabCredentials", description = "This is a interface for details of the Gitlab credentials")
public interface GitlabCredentialsDTO {}

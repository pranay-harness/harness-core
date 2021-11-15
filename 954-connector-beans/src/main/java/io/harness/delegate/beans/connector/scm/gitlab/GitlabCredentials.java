package io.harness.delegate.beans.connector.scm.gitlab;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitlabHttpCredentials.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = GitlabSshCredentials.class, name = GitConfigConstants.SSH)
})
public interface GitlabCredentials {}

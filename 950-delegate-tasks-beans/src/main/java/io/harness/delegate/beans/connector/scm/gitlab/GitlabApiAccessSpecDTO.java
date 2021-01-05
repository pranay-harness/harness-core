package io.harness.delegate.beans.connector.scm.gitlab;

import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = GitlabTokenSpecDTO.class, name = TOKEN) })
public interface GitlabApiAccessSpecDTO extends DecryptableEntity {}

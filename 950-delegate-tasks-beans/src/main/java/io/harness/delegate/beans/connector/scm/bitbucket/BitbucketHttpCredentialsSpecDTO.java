package io.harness.delegate.beans.connector.scm.bitbucket;

import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_PASSWORD;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = BitbucketUsernamePasswordDTO.class, name = USERNAME_AND_PASSWORD) })
public interface BitbucketHttpCredentialsSpecDTO {}

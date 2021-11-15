package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = BitbucketHttpCredentials.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = BitbucketSshCredentials.class, name = GitConfigConstants.SSH)
})
public interface BitbucketCredentials {}

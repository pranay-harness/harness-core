package io.harness.delegate.beans.connector.scm.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.
  Type(value = BitbucketUsernamePasswordApiAccessDTO.class, name = BitbucketConnectorConstants.USERNAME_AND_PASSWORD)
})
public interface BitbucketApiAccessSpecDTO {}

package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.
  Type(value = BitbucketUsernameTokenApiAccess.class, name = BitbucketConnectorConstants.USERNAME_AND_TOKEN)
})
public interface BitbucketApiAccessSpec extends DecryptableEntity {}

package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.
  Type(value = BitbucketUsernameTokenApiAccessDTO.class, name = BitbucketConnectorConstants.USERNAME_AND_TOKEN)
})
@Schema(name = "BitbucketApiAccess",
    description =
        "This contains details of the information such as references of username and password needed for Bitbucket API access")
public interface BitbucketApiAccessSpecDTO extends DecryptableEntity {}

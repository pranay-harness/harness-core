package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "BitbucketCredentials", description = "This is a interface for details of the Bitbucket credentials")
@JsonSubTypes({
  @JsonSubTypes.Type(value = BitbucketHttpCredentialsDTO.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = BitbucketSshCredentialsDTO.class, name = GitConfigConstants.SSH)
})
public interface BitbucketCredentialsDTO {}

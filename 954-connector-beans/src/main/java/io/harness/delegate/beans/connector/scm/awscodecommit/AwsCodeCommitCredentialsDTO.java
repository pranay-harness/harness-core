package io.harness.delegate.beans.connector.scm.awscodecommit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsCodeCommitHttpsCredentialsDTO.class, name = AwsCodeCommitConnectorConstants.HTTPS)
})
public interface AwsCodeCommitCredentialsDTO {}

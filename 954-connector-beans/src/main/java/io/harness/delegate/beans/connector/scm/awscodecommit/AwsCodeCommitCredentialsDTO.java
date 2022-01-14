package io.harness.delegate.beans.connector.scm.awscodecommit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsCodeCommitHttpsCredentialsDTO.class, name = AwsCodeCommitConnectorConstants.HTTPS)
})
@Schema(
    name = "AwsCodeCommitCredentials", description = "This interface for details of the AWS Code Commit credentials")
public interface AwsCodeCommitCredentialsDTO {}

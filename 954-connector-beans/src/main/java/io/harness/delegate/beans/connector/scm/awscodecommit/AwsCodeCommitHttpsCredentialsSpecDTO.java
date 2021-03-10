package io.harness.delegate.beans.connector.scm.awscodecommit;

import static io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorConstants.ACCESS_KEY_AND_SECRET_KEY;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes(
    { @JsonSubTypes.Type(value = AwsCodeCommitSecretKeyAccessKeyDTO.class, name = ACCESS_KEY_AND_SECRET_KEY) })
public interface AwsCodeCommitHttpsCredentialsSpecDTO extends DecryptableEntity {}
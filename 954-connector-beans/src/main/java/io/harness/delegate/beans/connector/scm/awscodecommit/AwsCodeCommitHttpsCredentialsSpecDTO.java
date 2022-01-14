package io.harness.delegate.beans.connector.scm.awscodecommit;

import static io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorConstants.ACCESS_KEY_AND_SECRET_KEY;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes(
    { @JsonSubTypes.Type(value = AwsCodeCommitSecretKeyAccessKeyDTO.class, name = ACCESS_KEY_AND_SECRET_KEY) })
@Schema(name = "AwsCodeCommitHttpsCredentialsSpec",
    description =
        "This contains details of the AWS Code Commit credentials specs such as references of username and password used via HTTPS connections")
public interface AwsCodeCommitHttpsCredentialsSpecDTO extends DecryptableEntity {}
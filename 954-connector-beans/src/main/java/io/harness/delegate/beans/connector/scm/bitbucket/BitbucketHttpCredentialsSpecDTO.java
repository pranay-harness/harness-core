package io.harness.delegate.beans.connector.scm.bitbucket;

import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.USERNAME_AND_PASSWORD;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = BitbucketUsernamePasswordDTO.class, name = USERNAME_AND_PASSWORD) })
@Schema(name = "BitbucketHttpCredentialsSpec",
    description =
        "This is a interface for details of the Bitbucket credentials Specs such as references of username and password")
public interface BitbucketHttpCredentialsSpecDTO extends DecryptableEntity {}

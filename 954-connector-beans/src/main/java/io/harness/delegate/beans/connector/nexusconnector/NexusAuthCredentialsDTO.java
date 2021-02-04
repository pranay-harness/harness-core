package io.harness.delegate.beans.connector.nexusconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@JsonSubTypes(
    { @JsonSubTypes.Type(value = NexusUsernamePasswordAuthDTO.class, name = NexusConstants.USERNAME_PASSWORD) })
@ApiModel("NexusAuthCredentials")
public interface NexusAuthCredentialsDTO extends DecryptableEntity {}

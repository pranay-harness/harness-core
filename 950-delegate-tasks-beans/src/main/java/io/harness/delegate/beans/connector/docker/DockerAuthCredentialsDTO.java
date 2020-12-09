package io.harness.delegate.beans.connector.docker;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonSubTypes({ @JsonSubTypes.Type(value = DockerUserNamePasswordDTO.class, name = DockerConstants.USERNAME_PASSWORD) })
public interface DockerAuthCredentialsDTO extends DecryptableEntity {}

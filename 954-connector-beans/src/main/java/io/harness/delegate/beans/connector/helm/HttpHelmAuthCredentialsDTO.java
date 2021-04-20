package io.harness.delegate.beans.connector.helm;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = HttpHelmUsernamePasswordDTO.class, name = HelmConstants.USERNAME_PASSWORD) })
public interface HttpHelmAuthCredentialsDTO extends DecryptableEntity {}

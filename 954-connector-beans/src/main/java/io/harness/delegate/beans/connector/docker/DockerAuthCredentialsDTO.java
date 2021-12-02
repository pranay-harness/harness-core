package io.harness.delegate.beans.connector.docker;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@OwnedBy(CDC)
@Schema(name = "DockerAuthCredentials", description = "This contains details of credentials for Docker Authentication")
@JsonSubTypes({ @JsonSubTypes.Type(value = DockerUserNamePasswordDTO.class, name = DockerConstants.USERNAME_PASSWORD) })
public interface DockerAuthCredentialsDTO extends DecryptableEntity {}

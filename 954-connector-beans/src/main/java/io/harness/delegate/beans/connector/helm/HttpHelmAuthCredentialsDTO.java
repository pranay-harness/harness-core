package io.harness.delegate.beans.connector.helm;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSubTypes({ @JsonSubTypes.Type(value = HttpHelmUsernamePasswordDTO.class, name = HelmConstants.USERNAME_PASSWORD) })
@Schema(name = "HttpHelmAuthCredentials", description = "This contains http helm auth credentials")
public interface HttpHelmAuthCredentialsDTO extends DecryptableEntity {}

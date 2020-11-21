package io.harness.delegate.beans.connector.docker;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public interface DockerAuthCredentialsDTO extends DecryptableEntity {}

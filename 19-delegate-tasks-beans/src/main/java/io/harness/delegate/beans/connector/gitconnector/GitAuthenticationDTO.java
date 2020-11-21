package io.harness.delegate.beans.connector.gitconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public abstract class GitAuthenticationDTO implements DecryptableEntity {}

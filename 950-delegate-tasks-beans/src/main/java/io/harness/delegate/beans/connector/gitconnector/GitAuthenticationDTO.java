package io.harness.delegate.beans.connector.gitconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitHTTPAuthenticationDTO.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = GitSSHAuthenticationDTO.class, name = GitConfigConstants.SSH)
})
public abstract class GitAuthenticationDTO implements DecryptableEntity {}

package io.harness.delegate.beans.connector.scm.genericgitconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitHTTPAuthentication.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = GitSSHAuthentication.class, name = GitConfigConstants.SSH)
})
public abstract class GitAuthentication implements DecryptableEntity {}

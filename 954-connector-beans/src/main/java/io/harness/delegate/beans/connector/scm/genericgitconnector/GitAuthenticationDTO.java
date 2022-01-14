package io.harness.delegate.beans.connector.scm.genericgitconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.scm.GitConfigConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitHTTPAuthenticationDTO.class, name = GitConfigConstants.HTTP)
  , @JsonSubTypes.Type(value = GitSSHAuthenticationDTO.class, name = GitConfigConstants.SSH)
})
@Schema(name = "GitAuthentication",
    description = "This is a interface for details of the Generic Git authentication information")
public abstract class GitAuthenticationDTO implements DecryptableEntity {}

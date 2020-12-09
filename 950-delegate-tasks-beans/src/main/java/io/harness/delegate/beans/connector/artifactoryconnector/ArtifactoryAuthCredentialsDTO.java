package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;

@JsonSubTypes({
  @JsonSubTypes.Type(value = ArtifactoryUsernamePasswordAuthDTO.class, name = ArtifactoryConstants.USERNAME_PASSWORD)
})
@ApiModel("ArtifactoryAuthCredentials")
public interface ArtifactoryAuthCredentialsDTO extends DecryptableEntity {}

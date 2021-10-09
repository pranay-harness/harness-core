package io.harness.delegate.beans.connector.artifactoryconnector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@OwnedBy(CDC)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArtifactoryUsernamePasswordAuthDTO.class, name = ArtifactoryConstants.USERNAME_PASSWORD)
})
@ApiModel("ArtifactoryAuthCredentials")
public interface ArtifactoryAuthCredentialsDTO extends DecryptableEntity {}

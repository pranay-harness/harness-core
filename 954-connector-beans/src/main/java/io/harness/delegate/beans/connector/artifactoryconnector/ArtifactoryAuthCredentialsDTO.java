package io.harness.delegate.beans.connector.artifactoryconnector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@OwnedBy(CDC)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArtifactoryUsernamePasswordAuthDTO.class, name = ArtifactoryConstants.USERNAME_PASSWORD)
})
@ApiModel("ArtifactoryAuthCredentials")
@Schema(name = "ArtifactoryAuthCredentials",
    description = "This entity contains the details of credentials for Artifactory Authentication")
public interface ArtifactoryAuthCredentialsDTO extends DecryptableEntity {}

package io.harness.delegate.beans.connector.nexusconnector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@OwnedBy(CDC)
@JsonSubTypes(
    { @JsonSubTypes.Type(value = NexusUsernamePasswordAuthDTO.class, name = NexusConstants.USERNAME_PASSWORD) })
@ApiModel("NexusAuthCredentials")
@Schema(name = "NexusAuthCredentials", description = "This contains details of credentials for Nexus Authentication")
public interface NexusAuthCredentialsDTO extends DecryptableEntity {}

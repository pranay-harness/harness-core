package io.harness.delegate.beans.connector.gcpconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSubTypes({ @JsonSubTypes.Type(value = GcpManualDetailsDTO.class, name = GcpConstants.MANUAL_CONFIG) })
@ApiModel("GcpCredentialSpec")
@Schema(name = "GcpCredentialSpec", description = "This contains GCP connector credentials spec")
public interface GcpCredentialSpecDTO extends DecryptableEntity {}

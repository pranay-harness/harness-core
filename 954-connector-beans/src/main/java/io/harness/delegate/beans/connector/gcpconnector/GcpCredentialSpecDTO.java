package io.harness.delegate.beans.connector.gcpconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = GcpManualDetailsDTO.class, name = GcpConstants.MANUAL_CONFIG) })
@ApiModel("GcpCredentialSpec")
public interface GcpCredentialSpecDTO extends DecryptableEntity {}

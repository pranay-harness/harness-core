package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@JsonSubTypes({
  @JsonSubTypes.Type(value = GcpDelegateDetailsDTO.class, name = GcpConstants.INHERIT_FROM_DELEGATE)
  , @JsonSubTypes.Type(value = GcpManualDetailsDTO.class, name = GcpConstants.MANUAL_CONFIG)
})
@ApiModel("GcpCredentialSpec")
public interface GcpCredentialSpecDTO {}

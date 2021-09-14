/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.gcpconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = GcpManualDetailsDTO.class, name = GcpConstants.MANUAL_CONFIG) })
@ApiModel("GcpCredentialSpec")
public interface GcpCredentialSpecDTO extends DecryptableEntity {}

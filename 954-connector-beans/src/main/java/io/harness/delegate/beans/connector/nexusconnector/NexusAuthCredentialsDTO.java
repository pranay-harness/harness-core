/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
@Schema(name = "NexusAuthCredentials",
    description = "This entity contains the details of credentials for Nexus Authentication")
public interface NexusAuthCredentialsDTO extends DecryptableEntity {}

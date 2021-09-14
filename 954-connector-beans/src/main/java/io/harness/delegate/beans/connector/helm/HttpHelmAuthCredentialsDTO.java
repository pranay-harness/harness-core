/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.helm;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = HttpHelmUsernamePasswordDTO.class, name = HelmConstants.USERNAME_PASSWORD) })
public interface HttpHelmAuthCredentialsDTO extends DecryptableEntity {}

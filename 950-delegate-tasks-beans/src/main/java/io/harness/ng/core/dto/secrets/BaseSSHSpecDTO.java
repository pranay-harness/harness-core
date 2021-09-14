/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.BaseSSHSpec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SSHConfigDTO.class, name = "SSH")
      , @JsonSubTypes.Type(value = KerberosConfigDTO.class, name = "Kerberos"),
    })
public abstract class BaseSSHSpecDTO {
  public abstract BaseSSHSpec toEntity();
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.params;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@SuperBuilder
public class ServiceEnvironmentParams extends ProjectParams {
  @NonNull String serviceIdentifier;
  @NonNull String environmentIdentifier;

  public static ServiceEnvironmentParamsBuilder builderWithProjectParams(ProjectParams projectParams) {
    return ServiceEnvironmentParams.builder()
        .orgIdentifier(projectParams.getOrgIdentifier())
        .accountIdentifier(projectParams.getAccountIdentifier())
        .projectIdentifier(projectParams.getProjectIdentifier());
  }
}

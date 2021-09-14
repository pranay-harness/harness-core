/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CrossAccountAccess {
  @NotNull private String crossAccountRoleArn;
  private String externalId;

  @Builder
  public CrossAccountAccess(String crossAccountRoleArn, String externalId) {
    this.crossAccountRoleArn = crossAccountRoleArn;
    this.externalId = externalId;
  }
}

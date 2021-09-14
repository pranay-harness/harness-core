/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyInfo {
  String appKeyId;
  String apiKeyName;

  public static EmbeddedUser getEmbeddedUserFromApiKey(ApiKeyInfo apiKeyInfo) {
    return EmbeddedUser.builder().name(apiKeyInfo.getApiKeyName() + " (Api Key)").build();
  }
}

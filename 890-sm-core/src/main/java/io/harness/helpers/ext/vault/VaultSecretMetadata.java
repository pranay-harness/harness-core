/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@OwnedBy(PL)
public class VaultSecretMetadata {
  @JsonProperty("current_version") private int currentVersion;
  @JsonProperty("updated_time") String updatedTime;
  private Map<Integer, VersionMetadata> versions;

  @Data
  public static class VersionMetadata {
    @JsonProperty("created_time") String createdTime;
    @JsonProperty("deletion_time") String deletionTime;
    boolean destroyed;
  }
}

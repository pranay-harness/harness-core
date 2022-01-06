/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml.storeConfig;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestStoreType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(HarnessTeam.CDP)
public enum StoreConfigType {
  @JsonProperty(ManifestStoreType.GIT) GIT(ManifestStoreType.GIT),
  @JsonProperty(ManifestStoreType.GITHUB) GITHUB(ManifestStoreType.GITHUB),
  @JsonProperty(ManifestStoreType.BITBUCKET) BITBUCKET(ManifestStoreType.BITBUCKET),
  @JsonProperty(ManifestStoreType.GITLAB) GITLAB(ManifestStoreType.GITLAB),
  @JsonProperty(ManifestStoreType.HTTP) HTTP(ManifestStoreType.HTTP),
  @JsonProperty(ManifestStoreType.S3) S3(ManifestStoreType.S3),
  @JsonProperty(ManifestStoreType.GCS) GCS(ManifestStoreType.GCS);
  private final String displayName;

  StoreConfigType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  @JsonCreator
  public static StoreConfigType getStoreConfigType(@JsonProperty("type") String displayName) {
    for (StoreConfigType storeConfigType : StoreConfigType.values()) {
      if (storeConfigType.displayName.equalsIgnoreCase(displayName)) {
        return storeConfigType;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", displayName, Arrays.toString(StoreConfigType.values())));
  }
}
